package {{BASE}}.{{feature}}.domain;

import {{BASE}}.shared.error.NotFoundException;

public class {{Entity}}NotFoundException extends NotFoundException {

    public {{Entity}}NotFoundException() {
        super("{{Entity}} not found.");
    }
}
