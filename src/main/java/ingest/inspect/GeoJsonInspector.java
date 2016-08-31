/**
 * Copyright 2016, RadiantBlue Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package ingest.inspect;

import ingest.utility.IngestUtilities;

import java.io.File;
import java.io.InputStream;
import model.data.DataResource;
import model.data.location.FileAccessFactory;
import model.data.type.GeoJsonDataType;
import model.job.metadata.SpatialMetadata;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geotools.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import util.PiazzaLogger;

/**
 * Inspects GeoJSON. Will parse the GeoJSON input to ensure validity, and parse
 * information such as spatial bounding box.
 * 
 * Vectors for GeoJSON will be stored in Piazza PostGIS table.
 * 
 * @author Sonny.Saniev, Patrick Doody, Russell Orf
 * 
 */
@Component
public class GeoJsonInspector implements InspectorType {
	@Value("${vcap.services.pz-geoserver-efs.credentials.postgres.hostname}")
	private String POSTGRES_HOST;
	@Value("${vcap.services.pz-geoserver-efs.credentials.postgres.port}")
	private String POSTGRES_PORT;
	@Value("${vcap.services.pz-geoserver-efs.credentials.postgres.database}")
	private String POSTGRES_DB_NAME;
	@Value("${vcap.services.pz-geoserver-efs.credentials.postgres.username}")
	private String POSTGRES_USER;
	@Value("${vcap.services.pz-geoserver-efs.credentials.postgres.password}")
	private String POSTGRES_PASSWORD;
	@Value("${vcap.services.pz-blobstore.credentials.access_key_id:}")
	private String AMAZONS3_ACCESS_KEY;
	@Value("${vcap.services.pz-blobstore.credentials.secret_access_key:}")
	private String AMAZONS3_PRIVATE_KEY;
	@Value("${postgres.schema}")
	private String POSTGRES_SCHEMA;

	private static final Integer DEFAULT_GEOJSON_EPSG_CODE = 4326;

	@Autowired
	private IngestUtilities ingestUtilities;
	@Autowired
	private PiazzaLogger logger;

	@Override
	public DataResource inspect(DataResource dataResource, boolean host) throws Exception {

		SpatialMetadata spatialMetadata = new SpatialMetadata();

		// Persist GeoJSON Features into the Piazza PostGIS Database.
		if (host && dataResource.getDataType() instanceof GeoJsonDataType) {

			SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) (new FeatureJSON()).readFeatureCollection(getFile(dataResource));
			FeatureSource<SimpleFeatureType, SimpleFeature> geojsonFeatureSource = new CollectionFeatureSource(featureCollection);
			ingestUtilities.persistFeatures(geojsonFeatureSource, dataResource);

			// Get the Bounding Box, set the Spatial Metadata
			ReferencedEnvelope envelope = geojsonFeatureSource.getBounds();
			spatialMetadata.setMinX(envelope.getMinX());
			spatialMetadata.setMinY(envelope.getMinY());
			spatialMetadata.setMaxX(envelope.getMaxX());
			spatialMetadata.setMaxY(envelope.getMaxY());
			spatialMetadata.setNumFeatures(geojsonFeatureSource.getFeatures().size());

//			// Get the SRS and EPSG codes
//			if (geojsonFeatureSource.getInfo().getCRS() != null) {
//				spatialMetadata.setCoordinateReferenceSystem(geojsonFeatureSource.getInfo().getCRS().toString());
//				spatialMetadata.setEpsgCode(CRS.lookupEpsgCode(geojsonFeatureSource.getInfo().getCRS(), true));
//			} else {
				// Default to EPSG 4326. Most GeoJSON is this code, and is
				// sort of an unofficial standard for GeoJSON.
				spatialMetadata.setEpsgCode(DEFAULT_GEOJSON_EPSG_CODE);
//			}

			// Populate the projected EPSG:4326 spatial metadata
			try {
				spatialMetadata.setProjectedSpatialMetadata(ingestUtilities.getProjectedSpatialMetadata(spatialMetadata));
			} catch (Exception exception) {
				exception.printStackTrace();
				logger.log(String.format("Could not project the spatial metadata for Data %s because of exception: %s",
						dataResource.getDataId(), exception.getMessage()), PiazzaLogger.WARNING);
			}

			// Convert DataType to postgis from geojson
			((GeoJsonDataType) dataResource.getDataType()).setDatabaseTableName(dataResource.getDataId());

			dataResource.spatialMetadata = spatialMetadata;
		}

		// Return DataResource
		return dataResource;
	}

	/**
	 * Gets the File for a Data Resource.
	 * 
	 * @param dataResource
	 *            data resource to pull file from
	 * @return File object
	 * @throws Exception
	 */
	private File getFile(DataResource dataResource) throws Exception {
		File file = new File(String.format("%s%s%s%s.%s", "tmp_geojson_", dataResource.getDataId(), File.separator,
				dataResource.getDataId(), "json"));
		FileAccessFactory fileFactory = new FileAccessFactory(AMAZONS3_ACCESS_KEY, AMAZONS3_PRIVATE_KEY);

		if (((GeoJsonDataType) dataResource.getDataType()).getLocation() != null) {
			InputStream fileStream = fileFactory.getFile(((GeoJsonDataType) dataResource.getDataType()).getLocation());
			FileUtils.copyInputStreamToFile(fileStream, file);
		} else {
			String geoJsonContent = ((GeoJsonDataType) dataResource.getDataType()).getGeoJsonContent();
			InputStream inputStream = IOUtils.toInputStream(geoJsonContent, "UTF-8");
			FileUtils.copyInputStreamToFile(inputStream, file);
		}

		return file;
	}	
}