package com.travel.service.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.travel.dto.HotelListData;
import com.travel.dto.Hotels;
import com.travel.service.HotelService;

/**
 * This Service implementation retrieves the masterList and filters the data based on parameters from form
 */

@Component(value = "hotelService")
public class HotelServiceImpl implements HotelService {
    private final static Logger LOGGER = Logger.getLogger(HotelServiceImpl.class.getName()); 

    private static List<Hotels> hotelArray = null;
    private List<Hotels> filteredList = new ArrayList<Hotels>();
    private List<Hotels> list1=new ArrayList<Hotels>();
    private List<Hotels> list2=new ArrayList<Hotels>();
    private static final String jsonFilePath = "http://deals.expedia.com/beta/deals/hotels.json";
    private static final String RATINGMINVALUE="0";
    private static final String RATINGMAXVALUE="5";
    private static final String LATITUDEMINVALUE="-85";
    private static final String LATITUDEMAXVALUE="85";
    private static final String LONGITUDEMAXVALUE="-120";
    private static final String LONGITUDEMINVALUE="120";

    //Retrieves the master List of all the deals from json and stores them in a list of object
    static {
        
        try {
            getCompleteList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     *{@link HotelService#getAllHotels(int, int)}
     */

    @Override
    public HotelListData getAllHotels(int pageNo, int offset) throws Exception {
        LOGGER.info("Entering getAllHotels");
        boolean moreResults = true;
        HotelListData hotelListData = new HotelListData();
        List<Hotels> paginatedHotelList = Collections.emptyList();
        int startLoc = pageNo * offset;
        if (filteredList != null && !filteredList.isEmpty()
                && startLoc <= filteredList.size() - 1) {
            paginatedHotelList = new ArrayList<Hotels>();
            int noelements = filteredList.size() - startLoc;
            if (noelements <= offset) {
                offset = noelements;
                moreResults = false;
            }
            for (int i = 0; i < offset; i++) {
                paginatedHotelList.add(filteredList.get(startLoc + i));
            }
        } else {
            moreResults = false;
        }
        LOGGER.info("Number of elements in page: "+pageNo + "are : " + paginatedHotelList.size());
        hotelListData.setPaginatedHotelList(paginatedHotelList);
        hotelListData.setMoreResults(moreResults);
        LOGGER.info("Exiting getAllHotels");

        return hotelListData;
    }
    
    /**
     *{@link HotelService#getFilteredData(boolean, Map, int, int)}
     */

    @Override
    public HotelListData getFilteredData(boolean filter, Map<String,String> param, int pageNo, int offset) throws Exception {
        LOGGER.info("Entering getFilteredData");
        if(pageNo==0) {
        if (!filter) {
            filteredList.clear();
            filteredList.addAll(hotelArray);
        }
        else {
               list1.clear();
               list1.addAll(hotelArray);
               list2.addAll(getCityFilteredList(param.get("city")));
               list1.clear();
               list1.addAll(list2);
               list2.clear();
               list2.addAll(getPriceFilteredList(param.get("price")));
               list1.clear();
               list1.addAll(list2);
               list2.clear();
               list2=getRangeFilteredList(param.get("guestratingmin"), param.get("guestratingmax"),"guestRating",RATINGMINVALUE,RATINGMAXVALUE);
               list1.clear();
               list1.addAll(list2);
               list2.clear();
               list2=getRangeFilteredList(param.get("starratingmin"), param.get("starratingmax"),"starRating",RATINGMINVALUE,RATINGMAXVALUE);
               list1.clear();
               list1.addAll(list2);
               list2.clear();
               list2=getRangeFilteredList(param.get("latitudemin"), param.get("latitudemax"),"latitude",LATITUDEMINVALUE,LATITUDEMAXVALUE);
               list1.clear();
               list1.addAll(list2);
               list2.clear();
               list2=getRangeFilteredList(param.get("longitudemin"), param.get("longitudemax"),"longitude",LONGITUDEMINVALUE,LONGITUDEMAXVALUE);
               list1.clear();
               list1.addAll(list2);
               list2.clear();
               Set<Hotels> filteredSet = new HashSet<Hotels>(list1);
               if(filteredList!=null) {
               filteredList.clear();
               }
               filteredList.addAll(filteredSet);
               LOGGER.info("Number of elements in final filtered list : "+filteredList.size());
        }
        }
        HotelListData hotelListData = getAllHotels(pageNo, offset);
        LOGGER.info("Exiting getFilteredData");
        return hotelListData;
    }
    
    private List<Hotels> getCityFilteredList(String data) throws Exception {
        LOGGER.info("Entering getCityFilteredList");
        List<Hotels> list=new ArrayList<Hotels>();
        if(StringUtils.isEmpty(data)) {
            return list1;
        }
        for (int i = 0; i < list1.size(); i++) {
                if (StringUtils.containsIgnoreCase(list1.get(i).getCity(), data)){
                    list.add(list1.get(i));
                }
    }
        LOGGER.info("Exiting getCityFilteredList");
        return list;
    }
    
    private List<Hotels> getPriceFilteredList(String data) throws Exception {
        LOGGER.info("Entering getPriceFilteredList");
        List<Hotels> list=new ArrayList<Hotels>();
        if(StringUtils.isEmpty(data)) {
            return list1;
        }
        for (int i = 0; i < list1.size(); i++) {
            if(list1.get(i).getTotalRate()<=Float.parseFloat(data)) {
                list.add(list1.get(i));
        }
    }
        LOGGER.info("Exiting getPriceFilteredList");
        return list;

}
    
    private List<Hotels> getRangeFilteredList(String min,String max,String param,String maxValue,String minValue) throws Exception {
        LOGGER.info("Entering getRangeFilteredList");
        List<Hotels> list=new ArrayList<Hotels>();
        if((StringUtils.isEmpty(min) && StringUtils.isEmpty(max))||(maxValue.equals(max)&&(minValue.equals(min)))) {
            return list1;
        }
        for (int i = 0; i < list1.size(); i++) {
            try {
                if((Float)PropertyUtils.getProperty(list1.get(i), param) >= Float.parseFloat(min) && (Float)PropertyUtils.getProperty(list1.get(i), param) <= Float.parseFloat(max)) {
                    list.add(list1.get(i));
                }
            } catch (NumberFormatException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            } 
    }
        LOGGER.info("Exiting getRangeFilteredList");
        return list;

}
    
    private static void getCompleteList() throws Exception {
            JsonReader reader = new JsonReader(new InputStreamReader(new URL(
                    jsonFilePath).openStream(), "UTF-8"));
            JsonArray userArray = new JsonParser().parse(reader)
                    .getAsJsonArray();
            if (userArray != null && userArray.isJsonArray()) {
                hotelArray = new ArrayList<Hotels>();
                for (JsonElement aUser : userArray) {
                    Hotels hotels = new Gson().fromJson(aUser, Hotels.class);
                    hotelArray.add(hotels);
                }
            }
            LOGGER.info("Number of all travel deals from json "+ hotelArray.size());
    }
    
}
