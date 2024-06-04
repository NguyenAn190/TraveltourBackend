package com.main.traveltour.dto.agent.hotel;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * DTO for {@link com.main.traveltour.entity.RoomTypes}
 */
@Data
public class RoomTypesDto implements Serializable {

    private String roomTypeName;
    private Integer capacityAdults;
    private Integer capacityChildren;
    private Integer amountRoom;
    private BigDecimal price;
    private String roomTypeDescription;
    private Boolean isDeleted = false;
    private Boolean breakfastIncluded;
    private Boolean freeCancellation;
}