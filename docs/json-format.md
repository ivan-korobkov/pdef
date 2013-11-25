Pdef JSON format
================

Pdef to JSON type mappings, all types are nullable.
<table>
    <tr>
        <th>Pdef type</th>
        <th>JSON type</th>
        <th>Examples</th>
    <tr>
    <tr>
        <td>bool</td>
        <td>`true`/`false`</td>
        <td>`true`, `false`</td>
    <tr>
    <tr>
        <td>int16, int32, int64, float, double</td>
        <td>number</td>
        <td>`-0.1234`</td>
    </tr>
    <tr>
        <td>string</td>
        <td>string</td>
        <td>`"Hello, world"`</td>
    </tr>
    <tr>
        <td>datetime</td>
        <td>string encoded as ISO8601 UTC datetime `yyyy-MM-ddTHH:mmZ`</td>
        <td>`"2013-11-26T17:59Z"`</td>
    </tr>
    <tr>
        <td>list</td>
        <td>array</td>
        <td>`[1, 2, 3]`</td>
    </tr>
    <tr>
        <td>set</td>
        <td>array</td>
        <td>`["a", "b", "c"]`</td>
    </tr>
    <tr>
        <td>map</td>
        <td>object with map keys converted to strings</td>
        <td>`{"key": "value"}`, `{"-1": "a"}`</td>
    </tr>
    <tr>
        <td>enum</td>
        <td>a lower-cased enum value string</td>
        <td>`"one"`, `"male"`</td>
    </tr>
    <tr>
        <td>message</td>
        <td>object, null fields should not be included</td>
        <td>`{"hello": "world"}`</td>
    </tr>
    <tr>
        <td>void</td>
        <td>null</td>
        <td>`null`</td>
    </tr>
</table>

Example pdef types:
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

Example JSON data:
```json
{
  "string0" : "hello",
  "bool0" : true,
  "int0" : 32,
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
