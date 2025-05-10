package com.zazhi.geoflow.mapper;

import com.zazhi.geoflow.entity.pojo.GeoFile;
import com.zazhi.geoflow.entity.pojo.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户
     */
    @Select("select * from user where username = #{username}")
    User getUserByUsername(String username);

    /**
     * 插入用户
     * @param user 用户
     */
    @Insert("insert into user(username, password) values(#{username}, #{password})")
    void insert(User user);

    /**
     * 根据id查询用户
     * @param id 用户id
     * @return 用户
     */
    @Select("select * from user where id = #{id}")
    User getUserById(Integer id);

    /**
     * 更新用户
     * @param user 用户
     * @return 用户
     */
    void update(User user);

}
