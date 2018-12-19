package pb.limiter.locklimiter.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import pb.limiter.locklimiter.model.Item;
import pb.limiter.locklimiter.model.ItemRepository;
import pb.limiter.locklimiter.model.ItemType;

@RunWith(SpringRunner.class)
@SpringBootTest
@ComponentScan
public class ItemServiceTest {
	
	@Inject
	ItemRepository itemRepository;
	
	@Inject
	ItemService itemService;
		
	Item server1, server2, storage1, storage2, datacenter;

	@Before
	public void setup(){

		server1= Item.newItem(ItemType.SERVER); 
		server2= Item.newItem(ItemType.SERVER); 
		storage1= Item.newItem(ItemType.STORAGE); 
		storage2= Item.newItem(ItemType.STORAGE); 
		datacenter= Item.newItem(ItemType.DATACENTER,server1, server2, storage1, storage2);
		itemRepository.saveAll(Arrays.asList(server1, server2, storage1, storage2, datacenter));
		showAll();
	}

	@Test
	@DirtiesContext
	public void testAttach() {
		itemService.attachStorageToServer(storage1.getId(), server1.getId());
		showAll();
		
		Item storage = itemRepository.findById(storage1.getId()).orElse(null);
		
		assertNotNull(storage);
		assertThat(storage.getItemType(), is(ItemType.STORAGE));
		assertThat(storage.getDependencies().size(), is(1));
		assertThat(storage.getDependencies().get(0).getItemType(), is(ItemType.SERVER));
		assertThat(storage.getDependencies().get(0).isLocked(), is(false));
		assertThat(storage.isLocked(), is(false));

	}

	@Test
	@DirtiesContext
	public void testLock() {
		itemService.attachStorageToServer(storage1.getId(), server1.getId());
		showAll();

		Item storage = itemRepository.findById(storage1.getId()).orElse(null);
		
		assertNotNull(storage);
		itemService.lockItem(storage, true);
		showAll();
		assertThat(storage.getDependencies().size(), is(1));
		assertThat(storage.getDependencies().get(0).isLocked(), is(true));
		assertThat(storage.isLocked(), is(true));
	}

	@Test
	@DirtiesContext
	public void testLockFailing() {
		itemService.attachStorageToServer(storage1.getId(), server1.getId());
		showAll();

		itemService.lockItem(server1, true);
		assertThat(storage1.isLocked(), is(false));
		assertThat(server1.isLocked(), is(true));
		
		boolean lockingOutcome = itemService.lockItem(storage1, true);
		assertThat(lockingOutcome, is(false));
				
		assertThat(storage1.isLocked(), is(false));
		assertThat(server1.isLocked(), is(true));
	}

	@Test
	@DirtiesContext
	public void testLockDataCenterFailing() {
		List<Item> datacenterInRepo = itemRepository.findByItemType(ItemType.DATACENTER);
		assertThat(datacenterInRepo.size(), is(1));

		Item datacenterRetrieved = datacenterInRepo.get(0);
		
		assertEquals(datacenter, datacenterRetrieved);

		itemService.attachStorageToServer(storage1.getId(), server1.getId());
		showAll();

		itemService.lockItem(server1, true);
		assertThat(storage1.isLocked(), is(false));
		assertThat(server1.isLocked(), is(true));
		assertThat(datacenter.isLocked(), is(false));
		
		boolean lockingOutcome = itemService.lockItem(server1, true);
		assertThat(lockingOutcome, is(false));
				
		assertThat(storage1.isLocked(), is(false));
		assertThat(server1.isLocked(), is(true));
		assertThat(datacenter.isLocked(), is(false));
	}

	@Test
	@DirtiesContext
	public void testLockDataCenter() {
		List<Item> datacenterInRepo = itemRepository.findByItemType(ItemType.DATACENTER);
		assertThat(datacenterInRepo.size(), is(1));

		Item datacenterRetrieved = datacenterInRepo.get(0);
		
		assertEquals(datacenter, datacenterRetrieved);

		itemService.lockItem(datacenterRetrieved, true);
		showAll();
		itemRepository.findAll().forEach(i->assertThat(i.isLocked(), is(true)));
	}

	@Test(expected=LockLimiterException.class)
	@DirtiesContext
	public void testAttachIncorrectly() {
		itemService.attachStorageToServer(server1.getId(), server2.getId());
	}

	private void showAll() {
		System.out.println();
		System.out.println("ALL ITEMS >>>> ");
		itemRepository.findAll().forEach(System.out::println);
	}

}
