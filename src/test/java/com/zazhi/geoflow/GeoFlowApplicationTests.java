package com.zazhi.geoflow;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.InputStream;

@SpringBootTest
class GeoFlowApplicationTests {

	@Autowired
	MinioClient minioClient;

	@Test
	void readTiffFromMinIO() throws Exception {
		InputStream inputStream = minioClient.getObject(
				GetObjectArgs.builder()
						.bucket("minio-upload-demo")
						.object("2025-03-29/c6b0d15d-ddbd-44ba-aa20-bffdf392dd01.TIF")
						.build()
		);
		GeoTiffReader reader = new GeoTiffReader(inputStream);
		System.out.println(reader.getFormat().getName());
		inputStream.close();
	}

	@Test
	void test(){
		new File("E:\\遥感数据\\NE2_50M_SR\\crop_test.tif").delete();
	}

}
