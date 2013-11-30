Pdef JSON format
================
Pdef is not strictly JSON-compatible in a way that it allows to use all types as root JSON objects,
not only objects or arrays. All types are nullable.
<table>
    <tr>
        <th style="width:20%">Pdef type</th>
        <th style="width:35%">JSON type</th>
        <th style="width:35%">Example</th>
    <tr>
    <tr>
        <td>bool</td>
        <td><code>true</code>/<code>false</code></td>
        <td><code>true</code>, <code>false</code></td>
    <tr>
    <tr>
        <td>int16, int32, int64, float, double</td>
        <td>number</td>
        <td><code>-0.1234</code>, <code>0</code></td>
    </tr>
    <tr>
        <td>string</td>
        <td>string</td>
        <td><code>"Hello, world"</code></td>
    </tr>
    <tr>
        <td>datetime</td>
        <td>string encoded as ISO8601 UTC <code>yyyy-MM-ddTHH:mm:ssZ</code></td>
        <td><code>"2013-11-26T17:59:17Z"</code></td>
    </tr>
    <tr>
        <td>list, set</td>
        <td>array</td>
        <td><code>[1, 2, 3]</code></td>
    </tr>
    <tr>
        <td>map</td>
        <td>object, string keys must be used as is, all other primitive keys must be converted to
        JSON strings</td>
        <td><code>map<int32, int32></code> is encoded as <code>{"-1": 123}</code></td>
    </tr>
    <tr>
        <td>enum</td>
        <td>lowercase enum value string</td>
        <td><code>"male"</code>, <code>"user_created"</code></td>
    </tr>
    <tr>
        <td>message</td>
        <td>object, null fields should be ignored</td>
        <td><code>{"name": "John", "age": 26}</code></td>
    </tr>
    <tr>
        <td>void</td>
        <td>null</td>
        <td><code>null</code></td>
    </tr>
</table>

### Polymorphic message deserialization
Given a base message or a subtype message in multi-level inheritance:

- Get a discriminator field.
- Deserialize it (it's an enum value).
- Get a subtype by the discriminator value.
- When no discriminator value or no subtype use the current message.
- Deserialize the JSON object as the subtype.

Given this Pdef messages:
```pdef
message EventType {
    USER_EVENT, USER_REGISTERED;
}

message Event {
    type        EventType @discriminator;
}

message UserEvent : Event(EventType.USER_EVENT) {
    userId      int32;
    userName    string;
}

message UserRegistered : UserEvent(EventType.USER_REGISTERED);
```

JSON data:
```
{
    "type": "user_registered",
    "userId": 10,
    "userName" "john"
}
```

Must be deserialized as the `UserRegistered` message.
```python
event0 = Event.from_json(s)          # Deserialize from the base type.
event1 = UserEvent.from_json(s)      # Deserialize from the subtype.
event2 = UserRegistered.from_json(s) # Deserialize from the multi-level subtype.

assert event0 == event1
assert event1 == event2
```



### Example
```pdef
enum TestEnum {
    ONE, TWO, THREE;
}

message TestMessage {
    string0     string;
    bool0       bool;
    int0        int32;
}

message TestComplexMessage : TestMessage {
    short0      int16;
    long0       int64;
    float0      float;
    double0     double;
    datetime0   datetime;

    list0       list<int32>;
    set0        set<int32>;
    map0        map<int32, float>;

    enum0       TestEnum;
    message0    TestMessage;
}
```

JSON:
```json
{
  "string0" : "hello",
  "bool0" : true,
  "int0" : -32,
  "short0" : 16,
  "long0" : 64,
  "float0" : 1.5,
  "double0" : 2.5,
  "list0" : [1, 2],
  "set0" : [1, 2],
  "map0" : {
    "1" : 1.5
  },
  "enum0" : "three",
  "message0" : {
    "string0" : "hello",
    "bool0" : true,
    "int0" : 16
  }
}
```
