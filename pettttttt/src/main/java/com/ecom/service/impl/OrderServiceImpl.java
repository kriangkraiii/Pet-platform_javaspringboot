package com.ecom.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ecom.model.Cart;
import com.ecom.model.OrderAddress;
import com.ecom.model.OrderRequest;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.repository.CartRepository;
import com.ecom.repository.ProductOrderRepository;
import com.ecom.service.OrderService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private ProductOrderRepository orderRepository;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private CommonUtil commonUtil;

	@Override
	public Map<String, Long> getOrderStatusCounts() {
		List<ProductOrder> allOrders = orderRepository.findAll();

		Map<String, Long> statusCounts = new HashMap<>();
		statusCounts.put("Total", (long) allOrders.size());
		statusCounts.put("In Progress",
				allOrders.stream().filter(order -> "In Progress".equals(order.getStatus())).count());
		statusCounts.put("Order Received",
				allOrders.stream().filter(order -> "Order Received".equals(order.getStatus())).count());
		statusCounts.put("Product Packed",
				allOrders.stream().filter(order -> "Product Packed".equals(order.getStatus())).count());
		statusCounts.put("Out for Delivery",
				allOrders.stream().filter(order -> "Out for Delivery".equals(order.getStatus())).count());
		statusCounts.put("Delivered",
				allOrders.stream().filter(order -> "Delivered".equals(order.getStatus())).count());
		statusCounts.put("Cancelled",
				allOrders.stream().filter(order -> "Cancelled".equals(order.getStatus())).count());

		return statusCounts;
	}

	@Override
	public ProductOrder getOrderById(Integer id) {
		return orderRepository.findById(id).orElse(null);
	}

	@Override
	public Boolean deleteOrder(Integer orderId) {
	    try {
	        ProductOrder order = orderRepository.findById(orderId).orElse(null);
	        if (order != null) {
	            orderRepository.delete(order);
	            return true;
	        }
	        return false;
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}

	@Override
	public void saveOrder(Integer userid, OrderRequest orderRequest) throws Exception {

		List<Cart> carts = cartRepository.findByUserId(userid);

		for (Cart cart : carts) {

			ProductOrder order = new ProductOrder();

			order.setOrderId(UUID.randomUUID().toString());
			order.setOrderDate(LocalDate.now());

			order.setProduct(cart.getProduct());
			order.setPrice(cart.getProduct().getDiscountPrice());

			order.setQuantity(cart.getQuantity());
			order.setUser(cart.getUser());

			order.setStatus(OrderStatus.IN_PROGRESS.getName());
			order.setPaymentType(orderRequest.getPaymentType());

			OrderAddress address = new OrderAddress();
			address.setFirstName(orderRequest.getFirstName());
			address.setLastName(orderRequest.getLastName());
			address.setEmail(orderRequest.getEmail());
			address.setMobileNo(orderRequest.getMobileNo());
			address.setAddress(orderRequest.getAddress());
			address.setCity(orderRequest.getCity());
			address.setState(orderRequest.getState());
			address.setPincode(orderRequest.getPincode());

			order.setOrderAddress(address);

			ProductOrder saveOrder = orderRepository.save(order);
			resetCart(cart.getUser());
			commonUtil.sendMailForProductOrder(saveOrder, "success");
		}
	}

	private void resetCart(UserDtls user) {
		cartRepository.deleteByUser(user);
	}

	@Override
	public List<ProductOrder> getOrdersByUser(Integer userId) {
		List<ProductOrder> orders = orderRepository.findByUserId(userId);
		return orders;
	}
	@Override
	public List<ProductOrder> getOrdersByProduct(Integer productId) {
	    return orderRepository.findByProductId(productId);
	}


	@Override
	public ProductOrder updateOrderStatus(Integer id, String status) {
		Optional<ProductOrder> findById = orderRepository.findById(id);
		if (findById.isPresent()) {
			ProductOrder productOrder = findById.get();
			productOrder.setStatus(status);
			ProductOrder updateOrder = orderRepository.save(productOrder);
			return updateOrder;
		}
		return null;
	}
	@Override
	public Double getTotalRevenue() {
	    List<ProductOrder> orders = orderRepository.findAll();
	    return orders.stream()
	            .filter(order -> !"Cancelled".equals(order.getStatus()))
	            .mapToDouble(order -> order.getPrice() * order.getQuantity())
	            .sum();
	}

	@Override
	public Double getTodayRevenue() {
	    LocalDate today = LocalDate.now();
	    List<ProductOrder> todayOrders = orderRepository.findByOrderDate(today);
	    return todayOrders.stream()
	            .filter(order -> !"Cancelled".equals(order.getStatus()))
	            .mapToDouble(order -> order.getPrice() * order.getQuantity())
	            .sum();
	}

	@Override
	public Integer getTodayOrdersCount() {
	    LocalDate today = LocalDate.now();
	    return orderRepository.findByOrderDate(today).size();
	}

	@Override
	public Integer getCountOrders() {
	    return (int) orderRepository.count();
	}

	@Override
	public List<Double> getDailyRevenueData(int days) {
	    List<Double> revenueData = new ArrayList<>();
	    LocalDate endDate = LocalDate.now();
	    
	    for (int i = days - 1; i >= 0; i--) {
	        LocalDate date = endDate.minusDays(i);
	        List<ProductOrder> dayOrders = orderRepository.findByOrderDate(date);
	        Double dayRevenue = dayOrders.stream()
	                .filter(order -> !"Cancelled".equals(order.getStatus()))
	                .mapToDouble(order -> order.getPrice() * order.getQuantity())
	                .sum();
	        revenueData.add(dayRevenue);
	    }
	    return revenueData;
	}

	@Override
	public List<String> getDailyRevenueLabels(int days) {
	    List<String> labels = new ArrayList<>();
	    LocalDate endDate = LocalDate.now();
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
	    
	    for (int i = days - 1; i >= 0; i--) {
	        LocalDate date = endDate.minusDays(i);
	        labels.add(date.format(formatter));
	    }
	    return labels;
	}

	@Override
	public List<Integer> getDailyOrdersData(int days) {
	    List<Integer> ordersData = new ArrayList<>();
	    LocalDate endDate = LocalDate.now();
	    
	    for (int i = days - 1; i >= 0; i--) {
	        LocalDate date = endDate.minusDays(i);
	        List<ProductOrder> dayOrders = orderRepository.findByOrderDate(date);
	        ordersData.add(dayOrders.size());
	    }
	    return ordersData;
	}

	@Override
	public List<String> getDailyOrdersLabels(int days) {
	    return getDailyRevenueLabels(days); // Same format
	}

	@Override
	public List<ProductOrder> getAllOrders() {
		return orderRepository.findAll();
	}

	@Override
	public Page<ProductOrder> getAllOrdersPagination(Integer pageNo, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return orderRepository.findAll(pageable);

	}

	@Override
	public ProductOrder getOrdersByOrderId(String orderId) {
		return orderRepository.findByOrderId(orderId);
	}

}
