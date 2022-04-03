package com.openfaas.function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openfaas.model.IRequest;
import com.openfaas.model.IResponse;
import com.openfaas.model.Response;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Handler extends com.openfaas.model.AbstractHandler {

    public IResponse Handle(IRequest req) {
        String responseBody = null;
        try {
            List<Fare> faresList = (new ObjectMapper()).readValue(req.getBody(), new TypeReference<>() {
            });
            List<Fare> eligibleFares = new ArrayList<>();
            Calendar c = Calendar.getInstance();

            Map<Integer, Long> tripDurationPerQuarter = new HashMap<>();
            tripDurationPerQuarter.put(1, 0L);
            tripDurationPerQuarter.put(2, 0L);
            tripDurationPerQuarter.put(3, 0L);
            tripDurationPerQuarter.put(4, 0L);

            faresList.forEach(fare -> {
                LocalDateTime pickupDate = formatDate(fare.getPickupDateTime());
                Date date = Date.from(pickupDate.atZone(ZoneId.systemDefault()).toInstant());
                c.setTime(date);
                if (c.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY &&
                        c.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY &&
                        (pickupDate.getHour() >= 7 && pickupDate.getHour() <= 10) ||
                        (pickupDate.getHour() >= 15 && pickupDate.getHour() <= 19)) {
                    enrichQuarterWithTripSeconds(tripDurationPerQuarter, fare);
                }
            });

            responseBody = (new ObjectMapper()).writeValueAsString(tripDurationPerQuarter);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        Response response = new Response();
        response.setBody(responseBody);
        return response;
    }

    private static LocalDateTime formatDate(String localDateTimeString) {
        return LocalDateTime.parse(localDateTimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static void enrichQuarterWithTripSeconds(Map<Integer, Long> frequencyPerQuarter, Fare fare) {
        double latitude = fare.getPickupLatitude();
        double longitude = fare.getPickupLongitude();
        if (longitude >= -73.99116516113281 && longitude <= -68.7915620803833 && latitude >= 39.72673797607412 && latitude <= 44.371944427490234) {
            frequencyPerQuarter.put(1, frequencyPerQuarter.get(1) + fare.getTripDuration());
        } else if (longitude >= -68.7915620803833 && longitude <= -63.59195899963379 && latitude >= 39.72673797607412 && latitude <= 44.371944427490234) {
            frequencyPerQuarter.put(2, frequencyPerQuarter.get(2) + fare.getTripDuration());
        } else if (longitude >= -73.99116516113281 && longitude <= -68.7915620803833 && latitude >= 35.0815315246582 && latitude <= 39.72673797607412) {
            frequencyPerQuarter.put(3, frequencyPerQuarter.get(3) + fare.getTripDuration());
        } else {
            frequencyPerQuarter.put(4, frequencyPerQuarter.get(4) + fare.getTripDuration());
        }
    }
}
