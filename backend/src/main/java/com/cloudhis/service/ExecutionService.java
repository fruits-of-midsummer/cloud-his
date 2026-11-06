package com.cloudhis.service;

import com.cloudhis.mapper.*;
import com.cloudhis.web.Requests;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.cloudhis.common.Values.*;
import static com.cloudhis.common.BusinessException.check;

@Service
public class ExecutionService {
    private final OrderMapper orders;private final AuthMapper users;private final AccessService access;private final AuthService auth;
    public ExecutionService(OrderMapper o,AuthMapper u,AccessService x,AuthService a){orders=o;users=u;access=x;auth=a;}
    @Transactional public void register(Map<String,Object> user,String id,Requests.Execute req) {
        Map<String,Object> order=must(orders.lock(id),"申请不存在");access.executor(user,order);
        check(Set.of("CHECK","LAB","TREAT").contains(str(order,"kind")),"非医技项目不能登记");
        Map<String,Object> executor=must(users.byId(req.executorId()),"执行医生不存在");
        check(active(executor) && str(executor,"role_code").equals(str(order,"kind")) && str(executor,"department_id").equals(str(order,"department_id")),"执行医生必须属于项目对应科室和岗位");
        check(orders.register(id,req.executorId())==1,"仅已收费且尚未登记的项目可以开始执行");auth.audit(user,"EXECUTION_REGISTER",id,"分配执行医生："+str(executor,"display_name"));
    }
    @Transactional public void result(Map<String,Object> user,String id,Requests.Result req) {
        Map<String,Object> order=must(orders.lock(id),"申请不存在");access.executor(user,order);
        check(str(user,"role_code").equals("ADMIN") || str(user,"id").equals(str(order,"executor_id")),"仅分配的执行医生可以录入结果");
        check(str(order,"execution_status").equals("REGISTERED") && str(order,"billing_status").equals("PAID"),"请先完成已收费项目登记，结果不能重复提交");
        orders.result(map("id",id(),"order_id",id,"content",req.content(),"author_id",str(user,"id")));orders.complete(id);
        auth.audit(user,"RESULT_CREATE",id,"录入执行结果");
    }
}
