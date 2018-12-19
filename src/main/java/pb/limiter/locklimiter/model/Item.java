package pb.limiter.locklimiter.model;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Item{
	
	@Enumerated(EnumType.STRING)
	private ItemType itemType;
	
	@Id 
	@GeneratedValue
	private long id;
	
	@OneToMany(fetch=FetchType.EAGER)
	@JoinColumn(referencedColumnName="id")
	private List<Item> dependencies;
	
	private boolean locked;
	
	public static Item newItem(ItemType type, Item... dependent){
		Item item= new Item();
		item.setItemType(type);
		item.setDependencies(Arrays.asList(dependent));
		return item;
	}
	
	public String toString(){
		StringBuilder deps=new StringBuilder();
		dependencies.stream().forEach(d->deps.append(d.getId()).append(", "));
		return String.format("{ id:%d, type:%s, locked:%s, dependencies:[%s] }",id,itemType,locked,deps);
	}
	
	public boolean equals(Object other){
		List<Item> dependencies = this.getDependencies();
		List<Item> otherDependencies = ((Item)other).getDependencies();
		return other!=null && other.getClass().equals(Item.class) && 
				this.getId()==((Item)other).getId() &&
				this.getItemType().equals(((Item)other).getItemType()) &&
				((dependencies==null && otherDependencies==null)
						||dependencies.containsAll(otherDependencies) &&
							otherDependencies.containsAll(dependencies));
	}
}