package com.adjuge.pattern;

import com.adjuge.model.ValidatorType;

public class BidValidatorFactory {
    public static BidValidationStrategy getValidator(ValidatorType rule) {

        // Code cực kỳ "sạch" với Switch-Case, chạy nhanh hơn if-else String rất nhiều
        return switch (rule) {
            case STANDARD -> new StandardBidValidator();
            case STEP_PRICE -> new FixedIncrementBidValidator();
            case VIP_JUMP -> new VipJumpBidValidator();
        };
    }
}
