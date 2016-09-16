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

import java.io.InputStream;

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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import ingest.utility.IngestUtilities;
import model.data.DataResource;
import model.data.location.FileAccessFactory;
import model.data.type.GeoJsonDataType;
import model.job.metadata.SpatialMetadata;
import util.PiazzaLogger;

/**
 * Inspects GeoJSON. Will parse the GeoJSON input to ensure validity, and parse information such as spatial bounding
 * box.
 * 
 * Vectors for GeoJSON will be stored in Piazza PostGIS table.
 * 
 * @author Sonny.Saniev, Patrick Doody, Russell Orf
 * 
 */
@Component
public class GeoJsonInspector implements InspectorType {
	@Value("${vcap.services.pz-blobstore.credentials.access_key_id:}")
	private String AMAZONS3_ACCESS_KEY;
	@Value("${vcap.services.pz-blobstore.credentials.secret_access_key:}")
	private String AMAZONS3_PRIVATE_KEY;

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
			FeatureJSON featureJSON = new FeatureJSON();
			InputStream geoJsonInputStream1 = getGeoJsonInputStream(dataResource);
			InputStream geoJsonInputStream2 = getGeoJsonInputStream(dataResource);

			SimpleFeatureType featureSchema = featureJSON.readFeatureCollectionSchema(geoJsonInputStream1, false);
			SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) featureJSON.readFeatureCollection(geoJsonInputStream2);
			FeatureSource<SimpleFeatureType, SimpleFeature> geojsonFeatureSource = new CollectionFeatureSource(featureCollection);
			ingestUtilities.persistFeatures(geojsonFeatureSource, dataResource, featureSchema);

			// Get the Bounding Box, set the Spatial Metadata
			ReferencedEnvelope envelope = geojsonFeatureSource.getBounds();
			spatialMetadata.setMinX(envelope.getMinX());
			spatialMetadata.setMinY(envelope.getMinY());
			spatialMetadata.setMaxX(envelope.getMaxX());
			spatialMetadata.setMaxY(envelope.getMaxY());
			spatialMetadata.setNumFeatures(geojsonFeatureSource.getFeatures().size());

			// Defaulting to 4326 since GeoTools has no FeatureSource available for GeoJSON files.
			spatialMetadata.setEpsgCode(DEFAULT_GEOJSON_EPSG_CODE);

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
			((GeoJsonDataType) dataResource.getDataType()).setMimeType(MediaType.APPLICATION_JSON_VALUE);

			dataResource.spatialMetadata = spatialMetadata;

			// Clean up resources
			featureJSON = null;
			geojsonFeatureSource = null;
			featureCollection = null;
			geoJsonInputStream1.close();
			geoJsonInputStream2.close();
		}

		// Return DataResource
		return dataResource;
	}

	/**
	 * Gets the Input Stream for a GeoJSON Resource
	 * 
	 * @param dataResource
	 *            data resource to pull input stream from
	 * @return File object
	 * @throws Exception
	 */
	private InputStream getGeoJsonInputStream(DataResource dataResource) throws Exception {
		FileAccessFactory fileFactory = new FileAccessFactory(AMAZONS3_ACCESS_KEY, AMAZONS3_PRIVATE_KEY);
		InputStream inputStream;

		if (((GeoJsonDataType) dataResource.getDataType()).getLocation() != null) {
			inputStream = fileFactory.getFile(((GeoJsonDataType) dataResource.getDataType()).getLocation());
		} else {
			String geoJsonContent = ((GeoJsonDataType) dataResource.getDataType()).getGeoJsonContent();
			inputStream = IOUtils.toInputStream(geoJsonContent, "UTF-8");
		}

		return inputStream;
	}
}