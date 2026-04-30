package com.adjuge.pattern;

import com.adjuge.model.*;
import java.util.UUID;

import java.util.HashMap;
import java.util.Map;
/*Dùng Factory + Builder Pattern giúp clean code, không sợ gán giá trị nhầm, dễ mở rộng*/
public class ItemFactory {

    // 1. Interface khuôn đúc với tên biến tường minh, rõ ràng
    private interface ItemCreator {
        Item render(String id, String name, String description, double startPrice,
                   String imageUrl, String condition, String... extraAttributes);
    }

    // 2. Kho Registry chứa các công thức đúc sản phẩm
    private static final Map<Category, ItemCreator> registry = new HashMap<>();

    static {
        // Đăng ký công thức cho Đồ điện tử
        registry.put(Category.ELECTRONICS, (id, name, description, startPrice, imageUrl, condition, extraAttributes) ->
                new Electronics.ElectronicsBuilder()
                        .setId(id).setName(name).setDescription(description)
                        .setStartPrice(startPrice).setImageUrl(imageUrl).setCondition(condition)
                        // Ép kiểu các thuộc tính phụ (extraAttributes) một cách an toàn
                        .setBrand(extraAttributes.length > 0 ? extraAttributes[0] : "Unknown")
                        .setModel(extraAttributes.length > 1 ? extraAttributes[1] : "Unknown")
                        .setWarrantyMonths(extraAttributes.length > 2 ? Integer.parseInt(extraAttributes[2]) : 0)
                        .build()
        );

        // Đăng ký công thức cho Xe cộ
        registry.put(Category.VEHICLES, (id, name, description, startPrice, imageUrl, condition, extraAttributes) ->
                new Vehicle.VehicleBuilder()
                        .setId(id).setName(name).setDescription(description)
                        .setStartPrice(startPrice).setImageUrl(imageUrl).setCondition(condition)
                        .setYearMade(extraAttributes.length > 0 ? Integer.parseInt(extraAttributes[0]) : 2024)
                        .setMake(extraAttributes.length > 1 ? extraAttributes[1] : "Unknown")
                        .setVehicleModel(extraAttributes.length > 2 ? extraAttributes[2] : "Unknown")
                        .setMileage(extraAttributes.length > 3 ? Integer.parseInt(extraAttributes[3]) : 0)
                        .build()
        );

        // Đăng ký công thức cho Nghệ thuật
        registry.put(Category.ART_COLLECTIBLES, (id, name, description, startPrice, imageUrl, condition, extraAttributes) ->
                new Art.ArtBuilder()
                        .setId(id).setName(name).setDescription(description)
                        .setStartPrice(startPrice).setImageUrl(imageUrl).setCondition(condition)
                        .setArtist(extraAttributes.length > 0 ? extraAttributes[0] : "Unknown")
                        .setYear(extraAttributes.length > 1 ? Integer.parseInt(extraAttributes[1]) : 2024)
                        .setMedium(extraAttributes.length > 2 ? extraAttributes[2] : "Unknown")
                        .build()
        );
    }

    /**
     * HÀM CHÍNH: Tạo sản phẩm (Áp dụng Polymorphism & Registry)
     */
    public static Item createItem(Category category, String name, String description,
                                  double startPrice, String imageUrl, String condition, String... extraAttributes) {

        // Tìm khuôn đúc tương ứng với danh mục trong kho
        ItemCreator creator = registry.get(category);

        if (creator == null) {
            throw new IllegalArgumentException("Error: Category " + category + " isn't supported yet");
        }
        String id = "i_" + UUID.randomUUID().toString().substring(0, 8);

        // Thực thi đa hình
        return creator.render(id, name, description, startPrice, imageUrl, condition, extraAttributes);
    }
}
