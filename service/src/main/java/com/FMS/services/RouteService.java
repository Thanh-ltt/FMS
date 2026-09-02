package com.FMS.services;

import com.FMS.dto.RouteDistanceDto;
import com.FMS.dto.request.RouteDistanceRequest;

public interface RouteService {
    RouteDistanceDto calculateDistance(RouteDistanceRequest request);
}
