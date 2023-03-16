/*
 * Copyright (c) 2023 AccelByte Inc. All Rights Reserved
 * This is licensed software from AccelByte Inc, for limitations
 * and restrictions contact your company contract manager.
 */
package net.accelbyte.extend.platform.demo.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SimpleSectionInfo {

    private String id;

    private List<SimpleItemInfo> items;

    public void WriteIntoToConsole()
    {
        System.out.println("Section Id: " + id);
        if ((items != null) && (items.size() > 0)) {
            for (SimpleItemInfo item: items) {
                System.out.println("\t" + item.getId() + " : " + item.getSku() + " : " + item.getTitle());
            }
        }
    }
}