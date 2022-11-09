package com.hsoft.practice;


import com.hsoft.api.MarketDataListener;
import com.hsoft.api.PricingDataListener;
import com.hsoft.api.VwapTriggerListener;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Entry point for the candidate to resolve the exercise
 */
public class VwapTrigger implements PricingDataListener, MarketDataListener {

  private final VwapTriggerListener vwapTriggerListener;

  public static HashMap<String, Double> fairValueMap = new HashMap<>();

  public static HashMap<String, ArrayList<Long>> ProductQuantityMap = new HashMap<>();

  public static HashMap<String, ArrayList<Double>> ProductPriceMap = new HashMap<>();

  public static HashMap<String, Integer> transactionNumber = new HashMap<>();


  public VwapTrigger(VwapTriggerListener vwapTriggerListener) {
    this.vwapTriggerListener = vwapTriggerListener;
  }


  @Override
  public synchronized void transactionOccurred(String productId, long quantity, double price) {

    if(transactionNumber.containsKey(productId))
      transactionNumber.replace(productId, transactionNumber.get(productId)+1);
    else
      transactionNumber.put(productId, 1);

//    System.out.println(productId+","+transactionNumber.get(productId));

    if(transactionNumber.containsKey(productId) && transactionNumber.get(productId) > 5){
      int tmp = transactionNumber.get(productId);
      while(tmp > 5){
        tmp -= 5;
      }
//      System.out.println("current index: "+ (tmp-1));
      ProductQuantityMap.get(productId).set(tmp-1, quantity);
      ProductPriceMap.get(productId).set(tmp-1, price);
    }
    else{
      if(ProductQuantityMap.containsKey(productId))
        ProductQuantityMap.get(productId).add(quantity);
      else{
        ProductQuantityMap.put(productId, new ArrayList<Long>());
        ProductQuantityMap.get(productId).add(quantity);
      }
      if(ProductPriceMap.containsKey(productId))
        ProductPriceMap.get(productId).add(price);
      else{
        ProductPriceMap.put(productId, new ArrayList<Double>());
        ProductPriceMap.get(productId).add(price);
      }
    }

    double vwap = vwapCalculator(productId);

    if(fairValueMap.containsKey(productId) && vwap > fairValueMap.get(productId)){
      vwapTriggerListener.vwapTriggered(productId, vwap, fairValueMap.get(productId));
    }

//    System.out.println("Transaction of " + productId + " and vwap is " + vwap);
  }

  @Override
  public synchronized void fairValueChanged(String productId, double fairValue) {
    if (ProductQuantityMap.containsKey(productId) && ProductPriceMap.containsKey(productId)) {
      double vwap = vwapCalculator(productId);
      if (vwap > fairValue)
        vwapTriggerListener.vwapTriggered(productId, vwap, fairValue);
    }
    if(!fairValueMap.containsKey(productId))
      fairValueMap.put(productId, fairValue);
    else
      fairValueMap.replace(productId, fairValue);
//    System.out.println("fair value of " + productId + " is " + fairValueMap.get(productId));
  }

  public static double vwapCalculator(String ProductId){
    ArrayList<Long> quantityAL = ProductQuantityMap.get(ProductId);
    ArrayList<Double> priceAL = ProductPriceMap.get(ProductId);

    long up = 0;
    long down = 0;
    for(int i = 0; i < quantityAL.size();i++){
      up += quantityAL.get(i)*priceAL.get(i);
      down += quantityAL.get(i);
    }
    return (double)up/down;
  }
}