package pb.limiter.locklimiter.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static pb.limiter.locklimiter.model.Action.CREATE;
import static pb.limiter.locklimiter.model.ItemType.SERVER;
import static pb.limiter.locklimiter.model.ItemType.STORAGE;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.BeanUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import pb.limiter.locklimiter.model.Action;
import pb.limiter.locklimiter.model.Item;
import pb.limiter.locklimiter.model.ItemRepository;
import pb.limiter.locklimiter.model.ItemType;
import pb.limiter.locklimiter.model.Request;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { RequestServiceTestConfiguration.class})
public class RequestServiceTest {

	@Inject
	FulfillmentServiceClient fulfillmentServiceClient;

	@Inject
	RequestService underTest;

	@Inject
	ItemService itemService;

	@Inject
	ItemRepository itemRepository;

	@Before
	public void setup() {
		assertNotNull(itemService);
		assertNotNull(underTest);
		assertNotNull(fulfillmentServiceClient);

		fulfillmentServiceClient=Mockito.spy(fulfillmentServiceClient);
		underTest.client=fulfillmentServiceClient;
		underTest=Mockito.spy(underTest);
	}

	@Test
	@Transactional
	@DirtiesContext
	public void testProcessRequest() {
		
		Item datacenter= Item.newItem(ItemType.DATACENTER);
		itemRepository.save(datacenter);
		assertNotNull(datacenter);
		assertFalse(datacenter.isLocked());

		Item datacenterStored = itemRepository.findById(datacenter.getId()).orElse(null);
		assertEquals(datacenter, datacenterStored);
		
		Request reqServer_1 = simulateRequestReceived(underTest, datacenter.getId(), SERVER, 0L, 0L, CREATE);
		Request reqServer_2 = simulateRequestReceived(underTest, datacenter.getId(), SERVER, 0L, 0L, CREATE);
		Request reqStorage_1 = simulateRequestReceived(underTest, datacenter.getId(), STORAGE, 0L, 0L, CREATE);

		simulateFulfillmentReceived(underTest, reqServer_1);
		assertFalse(itemRepository.getOne(reqServer_1.getItemId()).isLocked());
		
		assertTrue(underTest.getDeadLetterQueue().isEmpty());
		assertTrue(underTest.getSuspendedRequestQueueMap().get(reqServer_1.getItemId()).size()==0);
		assertTrue(underTest.getSuspendedRequestQueueMap().get(reqServer_2.getItemId()).size()==1);
		assertTrue(underTest.getSuspendedRequestQueueMap().get(reqStorage_1.getItemId()).size()==1);
	}

	@Test
	@DirtiesContext
	public void testProcessRequestFulfilled() {
	}

	private Request simulateRequestReceived(RequestService service, long dataCenterId, ItemType itemType, long itemId,
			long attachToServerId, Action action) {
		Request request = new Request();
		request.setDataCenterId(dataCenterId);
		request.setItemType(itemType);
		request.setItemId(itemId);
		request.setAttachToServerId(attachToServerId);
		request.setAction(action);

		System.out.println("New Request: " + request);

		service.processRequest(request);

		long requestItemId = request.getItemId();
		Item item = itemRepository.getOne(requestItemId);
		assertNotNull(item);
		assertTrue(item.isLocked());
		
		System.out.println("RequestService: " + service);
		return request;
	}

	private Request simulateFulfillmentReceived(RequestService service, Request request) {
		Request fulfilled = new Request();
		BeanUtils.copyProperties(request, fulfilled);
		
		System.out.println("Request fulfilled: " + fulfilled);

		service.processRequestFulfilled(fulfilled);
		System.out.println("RequestService: " + service);
		return request;
	}

}
