package ch.admin.bag.covidcertificate.log.cloudfoundry;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Provides root-level json attributes for cloud foundry app coordinates (org, space, app, instance), compatible with
 * the CF doppler/firehose log format.
 */
public class CloudFoundryAttributeProvider extends AbstractJsonProvider<ILoggingEvent> {
    private static final String CF_APP_ID = "cf_app_id";
    private static final String CF_APP_NAME = "cf_app_name";
    private static final String CF_SPACE_ID = "cf_space_id";
    private static final String CF_SPACE_NAME = "cf_space_name";
    private static final String CF_ORG_ID = "cf_org_id";
    private static final String CF_ORG_NAME = "cf_org_name";
    private static final String CF_INSTANCE_ID = "cf_instance_id";
    private static final String SOURCE_INSTANCE = "source_instance";
    private static final String SOURCE_TYPE = "source_type";
    private static final String APP_PROC_WEB = "APP/PROC/WEB";

    private boolean onCloudFoundry;
    private String spaceId;
    private String spaceName;
    private String organizationId;
    private String organizationName;
    private String applicationId;
    private String applicationName;
    private String instanceId;
    private String instanceIndex;

    @Override
    public void start() {
        super.start();

        String vcapApplication = System.getenv("VCAP_APPLICATION");
        if (vcapApplication != null) {
            onCloudFoundry = true;
            JsonParser jsonParser = JsonParserFactory.getJsonParser();
            Map<String, Object> map = jsonParser.parseMap(vcapApplication);
            applicationId = (String) map.get("application_id");
            applicationName = (String) map.get("application_name");
            spaceId = (String) map.get("space_id");
            spaceName = (String) map.get("space_name");
            organizationId = (String) map.get("organization_id");
            organizationName = (String) map.get("organization_name");
            instanceId = System.getenv("CF_INSTANCE_GUID");
            instanceIndex = System.getenv("CF_INSTANCE_INDEX");
        }
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent iLoggingEvent) throws IOException {
        if (onCloudFoundry) {
            JsonWritingUtils.writeStringField(generator, CF_APP_ID, applicationId);
            JsonWritingUtils.writeStringField(generator, CF_APP_NAME, applicationName);
            JsonWritingUtils.writeStringField(generator, CF_SPACE_ID, spaceId);
            JsonWritingUtils.writeStringField(generator, CF_SPACE_NAME, spaceName);
            JsonWritingUtils.writeStringField(generator, CF_ORG_ID, organizationId);
            JsonWritingUtils.writeStringField(generator, CF_ORG_NAME, organizationName);
            JsonWritingUtils.writeStringField(generator, CF_INSTANCE_ID, instanceId);
            JsonWritingUtils.writeStringField(generator, SOURCE_INSTANCE, instanceIndex);
            JsonWritingUtils.writeStringField(generator, SOURCE_TYPE, APP_PROC_WEB);
        }
    }
}
