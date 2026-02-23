package com.busco.localizacao.app.geo;

import ch.hsr.geohash.GeoHash;
import org.springframework.stereotype.Component;

@Component
public class GeoHashService {

    public String generate(double lat, double lng) {
        return GeoHash
                .withCharacterPrecision(lat, lng, 8)
                .toBase32();
    }
}