package pb.limiter.locklimiter.service;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.ToString;
import pb.limiter.locklimiter.model.Item;
import pb.limiter.locklimiter.model.ItemRepository;
import pb.limiter.locklimiter.model.Request;

@Transactional
@Component
@ToString(exclude = { "itemService", "client", "queueSize" })
public class RequestService {

	@Inject
	private ItemService itemService;

	@Inject
	private ItemRepository itemRepository;

	@Value("${item.requests.queue.size.max:10}")
	int queueSize;

	@Value("${lock.timeout.seconds:3}")
	long lockLimiterTimeout;

	Map<Long, BlockingQueue<Request>> suspendedRequestQueueMap = new ConcurrentHashMap<>();
	BlockingQueue<Request> deadLetterQueue = new PriorityBlockingQueue<Request>();

	public Map<Long, BlockingQueue<Request>> getSuspendedRequestQueueMap() {
		return suspendedRequestQueueMap;
	}

	public BlockingQueue<Request> getDeadLetterQueue() {
		return deadLetterQueue;
	}

	public long processRequest(@NotNull Request request) {
		try {
			createQueueAndItemIfNotExistent(request, request.getItemId());
			attemptForward(request);
			return request.getCreated();
		} catch (Throwable e) {
			deadLetterQueue.add(request);
			throw e;
		}
	}

	public void processRequestFulfilled(@NotNull Request request) {
		try {
			getItemService().lockItem(request.getItemId(), false);
			BlockingQueue<Request> suspendedRequestsForItem = suspendedRequestQueueMap.get(request.getItemId());
			suspendedRequestsForItem.remove();
			processQueue(suspendedRequestsForItem);
		} catch (Throwable e) {
			deadLetterQueue.add(request);
			throw e;
		}
	}

	private void createQueueAndItemIfNotExistent(@NotNull Request request, Long itemId) {
		if (!itemRepository.findById(itemId).isPresent()) {
			Item newItem = Item.newItem(request.getItemType());
			itemRepository.save(newItem);
			request.setItemId(newItem.getId());
			suspendedRequestQueueMap.put(newItem.getId(), new PriorityBlockingQueue<Request>(queueSize));
		}
	}

	private void attemptForward(@NotNull Request request) {
		BlockingQueue<Request> suspendedRequestsForItem = suspendedRequestQueueMap.get(request.getItemId());
		try {
			suspendedRequestsForItem.offer(request, 1000 * lockLimiterTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new LockLimiterException("timeout", e);
		}

		processQueue(suspendedRequestsForItem);
	}

	private void processQueue(@NotNull BlockingQueue<Request> queue) {
		synchronized (queue) {
			Request request = queue.peek();

			// if queue is empty, nothing to process for the item
			if (request == null) {
				return;
			}

			// if datacenter is locked no further requests would be processed
			if (itemRepository.getOne(request.getDataCenterId()).isLocked()) {
				return;
			}

			// if request's item is locked no further requests; wait to receive
			// the fulfillment
			if (itemRepository.getOne(request.getItemId()).isLocked()) {
				return;
			}

			if (itemService.lockItem(request.getItemId(), true)) {
				forwardToFulfillment(request);
			}
		}
	}

	@Value("${fulfillment.timeout.seconds:1}")
	int fulfillemtTimeout;

	@Inject
	FulfillmentServiceClient client;

	private void forwardToFulfillment(@NotNull Request request) {
		client.send(request, fulfillemtTimeout);
	}

	ItemService getItemService() {
		return itemService;
	}

	FulfillmentServiceClient getClient() {
		return client;
	}
}
