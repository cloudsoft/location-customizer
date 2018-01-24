/*
 * Copyright 2018 Cloudsoft Corporation Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cloudsoft.extras.location_customizer;

import static org.apache.brooklyn.core.location.LocationConfigKeys.CALLER_CONTEXT;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import org.apache.brooklyn.api.sensor.AttributeSensor;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.entity.EntityInternal;
import org.apache.brooklyn.core.sensor.Sensors;
import org.apache.brooklyn.location.jclouds.BasicJcloudsLocationCustomizer;
import org.apache.brooklyn.location.jclouds.JcloudsLocation;
import org.apache.brooklyn.util.core.config.ConfigBag;
import org.jclouds.compute.domain.NodeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts a key from @{see NodeMetadata.getUserMetadata} and adds a sensor with the value.
 *
 * <p>This relies on the entity having set the @{see CALLER_CONTEXT} flag on the location configuration. This is
 * usually, but not always, the case.</p>
 *
 * <p>Example:</p>
 *
 * <code>
 *
 * </code>
 */
public class UserMetadataValueToSensorLocationCustomizer extends BasicJcloudsLocationCustomizer {

    private static final ConfigKey<Map<String, String>> USER_METADATA = ConfigKeys.newConfigKey(new TypeToken<Map<String, String>>() {
    }, "location.userMetadata");
    private static final Logger LOG = LoggerFactory.getLogger(UserMetadataValueToSensorLocationCustomizer.class);
    private final String userMetadataKey;
    private final String sensorName;

    public UserMetadataValueToSensorLocationCustomizer(String userMetadataKey, String sensorName) {
        this.userMetadataKey = userMetadataKey;
        this.sensorName = sensorName;
    }

    @Override
    public void customize(JcloudsLocation location, NodeMetadata node, ConfigBag setup) {
        super.customize(location, node, setup);

        // Caller context - if set - is a reference to the entity that is using this location. It's not always set
        // though (there's not always a 1:1 reference between an entity and a location). Test for this condition and
        // gracefully exit if required.
        final Object obj = setup.get(CALLER_CONTEXT);
        if (obj == null || !(obj instanceof EntityInternal)) {
            LOG.info("No caller context available or context is not an EntityInternal. Sensor will not be added.");
            return;
        }

        final Map<String, String> userMetadata = node.getUserMetadata();
        if (userMetadata == null) {
            LOG.info("No userMetadata on the NodeMetadata. Sensor will not be added.");
            return;
        }

        EntityInternal entity = (EntityInternal) obj;
        AttributeSensor<String> sensor = Sensors.newStringSensor(sensorName);
        entity.sensors().set(sensor, userMetadata.get(userMetadataKey));
    }

}
