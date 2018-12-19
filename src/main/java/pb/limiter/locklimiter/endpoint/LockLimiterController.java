package pb.limiter.locklimiter.endpoint;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityNotFoundException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pb.limiter.locklimiter.model.Request;
import pb.limiter.locklimiter.service.ItemService;
import pb.limiter.locklimiter.service.LockLimiterException;
import pb.limiter.locklimiter.service.RequestService;

@RestController
public class LockLimiterController {

	@Inject
	RequestService requestService;

	@Inject
	ItemService itemService;

	@GetMapping(path = { "/items" })
	public HttpEntity<Object> items( @RequestParam(name = "item", required = false, defaultValue = "0") long item) {
		Map<String,Object> map=new HashMap<>();
		map.put("items", item == 0 ? itemService.findAll() : itemService.getOne(item));
		map.put("suspended-request-queue-map", requestService.getSuspendedRequestQueueMap());
		map.put("dead-letter-queue", requestService.getDeadLetterQueue());
		return new ResponseEntity<Object>(map, HttpStatus.OK);
	}

	@PostMapping(path = { "/requests" })
	public HttpEntity<Object> processRequests(@RequestBody Request[] requests) {
		Object result = Arrays.asList(requests)
			.stream()
			.map(requestService::processRequest)
			.collect(Collectors.toList());
		return new ResponseEntity<Object>(result, HttpStatus.ACCEPTED);
	}

	@PostMapping(path = { "/fulfillment" })
	public HttpStatus processRequestFulfilled(@RequestBody Request request) {
		requestService.processRequestFulfilled(request);
		return HttpStatus.ACCEPTED;
	}

	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Item not found")
	@ExceptionHandler(EntityNotFoundException.class)
	public void notFound() {
	}

	@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Illegal value for argument")
	@ExceptionHandler(IllegalArgumentException.class)
	public void illegalArgument() {
	}

	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Internal locking error")
	@ExceptionHandler(LockLimiterException.class)
	public void lockLimiterError() {
	}

	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Generic/internal server error")
	@ExceptionHandler(Throwable.class)
	public void generic() {
	}
}