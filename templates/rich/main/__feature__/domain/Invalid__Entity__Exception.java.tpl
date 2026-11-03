package {{BASE}}.{{feature}}.domain;

import {{BASE}}.shared.error.BusinessRuleException;

/** A {{entity}} rule was broken (invariant or illegal state change). */
public class Invalid{{Entity}}Exception extends BusinessRuleException {

    public Invalid{{Entity}}Exception(String message) {
        super(message);
    }
}
