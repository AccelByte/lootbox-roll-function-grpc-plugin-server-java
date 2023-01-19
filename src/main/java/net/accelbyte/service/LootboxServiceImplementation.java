package net.accelbyte.service;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.accelbyte.platform.entitlement.lootbox.v1.*;
import org.lognet.springboot.grpc.GRpcService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@GRpcService
public class LootboxServiceImplementation extends LootBoxGrpc.LootBoxImplBase {

    @Override
    public void rollLootBoxRewards(RollLootBoxRewardsRequest request, StreamObserver<RollLootBoxRewardsResponse> responseObserver) {
        log.info("Received rollLootBoxRewards request");

        List<RewardObject> finalItems = new ArrayList<>();

        List<LootBoxItemInfo.LootBoxRewardObject> rewards = request.getItemInfo().getLootBoxRewardsList();
        int rewardWeightSum = 0;
        for (LootBoxItemInfo.LootBoxRewardObject reward: rewards) {
            rewardWeightSum += reward.getWeight();
        }

        for (int i=0;i<request.getQuantity();i++)
        {
            int selIdx = 0;
            for (double r = Math.random() * rewardWeightSum; selIdx < rewards.size() - 1; ++selIdx) {
                r -= rewards.get(selIdx).getWeight();
                if (r <= 0.0)
                    break;
            }

            LootBoxItemInfo.LootBoxRewardObject selReward = rewards.get(selIdx);
            int itemCount = selReward.getItemsCount();

            int selItemIdx = (int)Math.round(Math.random() * (itemCount - 1));
            BoxItemObject selItem = selReward.getItems(selItemIdx);

            RewardObject rewardItem = RewardObject
                    .newBuilder()
                    .setItemId(selItem.getItemId())
                    .setItemSku(selItem.getItemSku())
                    .setCount(selItem.getCount())
                    .build();
            finalItems.add(rewardItem);
        }

        RollLootBoxRewardsResponse response = RollLootBoxRewardsResponse
                .newBuilder()
                .addAllRewards(finalItems)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
