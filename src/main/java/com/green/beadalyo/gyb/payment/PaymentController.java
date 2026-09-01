package com.green.beadalyo.gyb.payment;

import com.green.beadalyo.gyb.sse.SSEApiController;
import com.green.beadalyo.jhw.security.MyUser;
import com.green.beadalyo.jhw.security.MyUserDetails;
import com.green.beadalyo.jhw.security.jwt.JwtTokenProvider;
import com.green.beadalyo.jhw.user.UserServiceImpl;
import com.green.beadalyo.jhw.user.entity.User;
import com.green.beadalyo.lmy.order.OrderService;
import com.green.beadalyo.lmy.order.entity.Order;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@Hidden
@RestController
@RequestMapping("payment")
@RequiredArgsConstructor
public class PaymentController
{

    private final PaymentService service ;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserServiceImpl userService ;
    private final OrderService orderService ;
    private final SSEApiController sse ;

    @PostMapping("webhook")
    public void webhook(@RequestBody PaymentRequest request) throws Exception
    {

        if (request.getType().equals("Transaction.Ready")) return ;
        PaymentData data = service.getData(request.getData().getPaymentId()) ;

        UserDetails userDetails =  jwtTokenProvider.getUserDetailsFromToken(data.getCustomer().getId()) ;
        MyUser myUser = ((MyUserDetails)userDetails).getMyUser();
        User user = userService.getUser(myUser.getUserPk()) ;
        long paymentId = Long.parseLong(data.getCustomData());
        Order order = orderService.getOrderByOrderPk(paymentId) ;
        if (order.getOrderUser() != user) order.setOrderState(-1); ; // 실패시 로깅처리
//        if ((order.getOrderPrice() - order.getUseMileage()) != data.getAmount().getTotal()) return ;
//        user.setMileage(order.getUseMileage());
//        userService.save(user);
        order.setOrderState(2);
        orderService.saveOrder(order);
        User user2 = order.getOrderRes().getUser() ;
        sse.sendEmitters("OrderRequest",user2.getUserPk());


    }
}
