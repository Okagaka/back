package com.okagaka.OkaGaka.common.external.tmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.okagaka.OkaGaka.common.external.tmap.TmapMatrixClient;
import com.okagaka.OkaGaka.common.external.tmap.Coordinate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatrixTestService {

    private final TmapMatrixClient tmapMatrixClient;

    public MatrixTestService(TmapMatrixClient tmapMatrixClient) {
        this.tmapMatrixClient = tmapMatrixClient;
    }

    public void testMatrix() {
        List<Coordinate> origins = List.of(
                new Coordinate("33.47430580", "126.89841063"),
                new Coordinate("33.40489690", "126.90455058")
        );

        List<Coordinate> destinations = List.of(
                new Coordinate("33.45241976", "126.92468664")
        );

        var result = tmapMatrixClient.getMatrix(origins, destinations);

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            String json = mapper.writeValueAsString(result);
            System.out.println("🔍 Matrix API 결과:\n" + json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

