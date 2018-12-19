package pb.limiter.locklimiter.service;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;

import pb.limiter.locklimiter.model.Item;
import pb.limiter.locklimiter.model.ItemRepository;
import pb.limiter.locklimiter.model.ItemType;

@Transactional
@Component
public class ItemService {

	@Inject
	ItemRepository itemRepository;

	public boolean lockItem(long itemId, boolean lock) {
		return this.lockItem(itemRepository.getOne(itemId), lock);
	}

	public boolean lockItem(@NotNull Item item, boolean lock) {
		item.setDependencies(itemRepository.getOne(item.getId()).getDependencies());
		switch (item.getItemType()) {
		case DATACENTER:
			if (item.getDependencies().stream().filter(i -> {
				return !this.lockItem(i, lock);
			}).findAny().isPresent()) {
				return false;
			}
			break;
		case STORAGE:
			if (item.getDependencies().stream().filter(i -> {
				return !this.attemptLock(i, lock);
			}).findAny().isPresent()) {
				return false;
			}
			break;
		default:
			break;
		}
		return attemptLock(item, lock);
	}

	public void attachStorageToServer(Long storageId, Long serverId) {
		Item first = itemRepository.findById(storageId).orElse(null);
		Item second = itemRepository.findById(serverId).orElse(null);
		if (first == null || first.getItemType() != ItemType.STORAGE) {
			throw new LockLimiterException("invalid storage id " + first.getId());
		}
		first.getDependencies().clear();
		attachToFirstItemOtherItems(first, second);
	}

	public void attachToFirstItemOtherItems(@NotNull Item first, @NotNull Item... second) {
		for (Item i : second) {
			first.getDependencies().add(i);
		}
		itemRepository.save(first);
	}

	public Object findAll() {
		return itemRepository.findAll();
	}

	public Object getOne(long item) {
		return itemRepository.getOne(item);
	}

	private boolean attemptLock(Item item, boolean lock) {
		if (item.isLocked() && lock) {
			return false;
		}
		item.setLocked(lock);
		itemRepository.save(item);
		return true;
	}
}