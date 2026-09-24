package com.example.bai1__ss17.service;

import com.example.bai1__ss17.model.Dish;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FoodService {

    /**
     * Lấy danh sách món ăn theo restaurantId.
     * Sử dụng @Cacheable để lưu kết quả vào Redis cache.
     * Ở lần gọi đầu tiên, phương thức sẽ thực thi (truy vấn DB).
     * Ở các lần gọi tiếp theo với cùng restaurantId, dữ liệu sẽ được lấy trực tiếp từ Redis.
     */
    @Cacheable(value = "restaurantDishes", key = "#restaurantId")
    public List<Dish> getDishesByRestaurant(Long restaurantId) {
        System.out.println("------------------------------------------------------------------");
        System.out.println(">>> [DATABASE HIT] Đang truy vấn CSDL cho nhà hàng ID: " + restaurantId);
        System.out.println("------------------------------------------------------------------");

        List<Dish> dishes = new ArrayList<>();
        if (restaurantId == 1L) {
            dishes.add(new Dish(1L, "Phở Bò Tái Nạm", 55000.0, "Nước dùng thơm ngon đậm đà, thịt bò mềm"));
            dishes.add(new Dish(2L, "Bún Chả Hà Nội", 50000.0, "Thịt nướng than hoa, nước mắm tỏi ớt đặc trưng"));
            dishes.add(new Dish(3L, "Bánh Mì Kẹp Thịt", 25000.0, "Bánh mì giòn rụm kẹp pate và chả lụa"));
        } else {
            dishes.add(new Dish(4L, "Cơm Tấm Sườn Bì Chả", 60000.0, "Sườn nướng mật ong sốt đậm vị"));
            dishes.add(new Dish(5L, "Trà Sữa Trân Châu Đường Đen", 35000.0, "Trà sữa đậm vị trà cùng trân châu dẻo"));
        }
        return dishes;
    }
}
