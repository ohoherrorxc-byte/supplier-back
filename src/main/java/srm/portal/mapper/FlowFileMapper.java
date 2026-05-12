package srm.portal.mapper;

import org.apache.ibatis.annotations.*;
import srm.portal.entity.FlowFile;

@Mapper
public interface FlowFileMapper {

    @Insert("INSERT INTO tb_flow_file (business_id, business_name, url, name, create_user, create_time, status, is_deleted) " +
            "VALUES (#{businessId}, #{businessName}, #{url}, #{name}, #{createUser}, NOW(), #{status}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FlowFile flowFile);
}