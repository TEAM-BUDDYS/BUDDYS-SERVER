package org.sopt.buddys.domain.place.client.dto;

public record GoogleCircle(
    GoogleLatLng center,
    double radius
) {
}