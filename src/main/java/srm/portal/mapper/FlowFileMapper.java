package srm.portal.mapper;

import org.apache.ibatis.annotations.*;
import srm.portal.entity.FlowFile;

@Mapper
public interface FlowFileMapper {

    @Insert("INSERT INTO tb_flow_file (id, business_id, business_name, url, name, create_user, create_time, status, is_deleted) " +
            "VALUES (#{id}, #{businessId}, #{businessName}, #{url}, #{name}, #{createUser}, NOW(), #{status}, 0)")
    int insert(FlowFile flowFile);
}