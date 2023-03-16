package net.accelbyte.extend.platform.demo.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SimpleLootboxItem {

    private String id;

    private String sku;

    private String title;

    private String diff;

    private List<SimpleItemInfo> rewardItems;

    public void writeIntoToConsole()
    {
        System.out.println("Lootbox Item Id: " + id);
        System.out.println("Reward Items:");
        if ((rewardItems != null) && (rewardItems.size() > 0)) {
            for (SimpleItemInfo item: rewardItems) {
                System.out.println("\t" + item.getId() + " : " + item.getSku() + " : " + item.getTitle());
            }
        }
    }
}
