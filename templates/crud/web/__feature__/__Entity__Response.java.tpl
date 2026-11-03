package {{BASE}}.{{feature}};

import java.util.UUID;

record {{Entity}}Response(UUID id, String name) {

    static {{Entity}}Response from({{Entity}} {{entity}}) {
        return new {{Entity}}Response({{entity}}.getId(), {{entity}}.getName());
    }
}
