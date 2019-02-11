[![Build Status](https://travis-ci.org/madama/jalia.svg?branch=master)](https://travis-ci.org/madama/jalia)


Jalia
=====

JSON Serializer/Deserializer with Java Beans compliant behaviour.

What is Jalia?
--------------

Jalia is a library to transform Java objects (POJO, entities, lists, maps etc..) to JSON and vice versa.

How is Jalia better than others?
--------------------------------

Jalia is Java Beans and entity oriented, meaning that:
- it will use your getters and setters
- it will respect your List and Map instances
- it will use your entities by matching them by id
- it is compatible with Spring, Spring Boot, JPA using Hibernate and other providers, Lombok, AspectJ and other similar
utilities
- it supports polymorphism and other Java OO features
- it is fast, taking 0.01 milliseconds on single core on both serialization and deserialization of beans.

Also, when producing Json, the set of fields to serialize can be customized, eventually by the front end
itself, also on JPA relations, making it obsolete to define the JSON fields to be sent request per request.

How can I use Jalia in my project?
----------------------------------

You have three ways of using Jalia:
- as a library, using the ObjectMapper to serialize and deserialize JSON programmatically
- in a plain Spring application, configuring the JaliaHttpMessageConverter and the JaliaParametersFilter
- in a Spring Boot application, using Jalia Spring Boot support

Serialization
====================

Let's consider a simple Entity:

```java
@Entity
public class User {

  @Id
  private Long id;
  private String firstName;
  private String lastName;

  public String getFirstName() {
    return this.firstName;
  }
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return this.lastName;
  }
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
}
```

Let's suppose we want to return a User from an API call:

```java
@RestController @RequestMapping("/users")
public class UserController {

  @Autowired
  private UserJpaRepository users;  // Obviously you can use a service if you prefer

  @GetMapping("/{id}")
  public User getUser(Long id) {
    return users.findById(id);
  }

}
```

This will serialize the User entity when a GET request is received:

```
GET /users/1

{
  "@entity": "User",
  "id": 1,
  "firstName": "John",
  "lastName": "Smith"
}
```

Where:
- "@entity" is the entity type, can be used by the frontend to recognize entity type in more complex domains
with polymorphism.
- "id" is the entity id, always serialized
- other entries are the properties of our class.

Customizing serialization
-------------------------

Now, let's suppose that our User entity evolves with new fields:

```java
@Entity
public class User {

  // Previous fields omitted

  private String emailAddress;
  private String password;

  @JsonGetter("email")
  public String getEmailAddress() {
    return this.emailAddress;
  }

  @JsonIgnore
  public String getPassword() {
    return this.password;
  }

  // Other getters and setters omitted

}
```

Our java property is `emailAddress`, but on JSON we prefer a simple "email", so we annotate the `getEmailAddress`
method with @JsonGetter specifying the name we want to use on JSON.

Obviously, we don't want to serialize to the front end the password of our users, so annotating the getter with
@JsonIgnore makes Jalia ignore that property altogether. We can also use class-level @JsonIgnoreProperties to
specify a list of properties we want ignored.

Front end requested fields
--------------------------

By installing the JaliaFilter (which is done automatically on Spring Boot), the frontend can specify which fields
it needs to be serialized. For example:

```
GET /users/1?fields=firstName,emailAddress

{
  "@entity": "User",
  "id": 1,
  "firstName": "John",
  "emailAddress": "john@nonexistent.com"
}
```

This doesn't sound very exciting with such a simple model, but read below.

Relations
---------

One great advantage of JPA is using relations transparently in out model. For example:

```java
@Entity
public class User {
  // Previous fields omitted
  @OneToMany
  private List<Address> addresses;

  @ManyToMany(mappedBy="members")
  private Set<Group> memberOf;

  @ManyToMany(mappedBy="following")
  private List<User> followedBy;

  @ManyToMany(mappedBy="followedBy")
  private List<User> following;

  @JsonOnDemandOnly
  public Set<Group> getMemberOf() {
    return this.memberOf;
  }

  @JsonOnDemandOnly
  public List<User> getFollowedBy() {
    return this.followedBy;
  }

  @JsonOnDemandOnly
  public List<User> getFollowing() {
    return this.following;
  }

  // Other getters and setters omitted
}
```

Relations are great modeling tool, but they just don't cope with common serialization to JSON. If we serialize
those fields, for each user we are serializing his groups, and for each of these groups we are serializing the whole
list of other users, and so on until we serialize the entire database at once.

The common pattern with other serialization engines is to provide only basic fields, and then create other endpoints,
more code into the controllers, to make the frontend require the list of groups, the list of followers etc..

However, in Jalia is the frontend asking which fields must be serialized, so depending on what the frontend need
it can easily ask specifically for those fields:

```
GET /users/1?fields=firstName,groups.name

{
  "@entity": "User",
  "id": 1,
  "firstName": "John",
  "groups": [
    {
      "@entity": "Group",
      "id": 12,
      "name": "Demo users"
    },
    {
      "@entity": "Group",
      "id": 45,
      "name": "Adminsitrators"
    }
  ]
}


GET /users/1?fields=followedBy.firstName,following.firstName

{
  "@entity": "User",
  "id": 1,
  "followedBy": [
    {
      "@entity": "User",
      "id": 2,
      "firstName": "Marc"
    },
    {
      "@entity": "User",
      "id": 15,
      "firstName": "Bob"
    }
  ],
  "following": [
    {
      "@entity": "User",
      "id": 2,
      "firstName": "Marc"
    },
    {
      "@entity": "User",
      "id": 26,
      "firstName": "Jacqueline"
    },
    {
      "@entity": "User",
      "id": 33,
      "firstName": "Sarah"
    }
  ]
}

GET /users/1

{
  "@entity": "User",
  "id": 1,
  "firstName": "John",
  "lastName": "Smith",
  "addresses": [
    {
      "@entity": "Address",
      "id": 7,
      "street": "Long road",
      "number": 25,
      "zip": "12345"
    }
  ]
}
```

This system gives the following advantages:
- use relations, serialize directly the model, no need for DTOs
- no need for additional controller methods, model is navigable on demand
- serialized JSON is always consistent

And note that, up to here, we haven't yet added a single line to our controller.

More formally:
- by default, if nothing is specified, all properties are serialized, like in every serialization library
- which fields are serialized by default can be specified with the class-level annotation @JsonDefaultFields
- if the frontend requested a specific list of fields, only those fields are serialized
- requested fields can traverse the relations simply using a dot notation
- properties annotated with @JsonOnDemandOnly are serialized
- properties annotated with @JsonIgnore are never serialized

Fields and groups
-----------------

The "fields" parameter is very powerful, leaving it open in production on a complex model can lead
to leaving too much power to untrusted clients. Moreover, on a complex frontend, the list of fields
can get very long very quickly.

To solve both these problems, Jalia supports field groups.

A group file defines some field groups like for example:

```json
{
  "profileScreen": "firstName,lastName,addresses.*",
  "followersScreen": {
    "followebBy": "firstName,lastName"
  }
}
```

This file can be:
- if using Spring Boot, in src/main/resources/jalia/group*.json
- otherwise, load it as you wish and use OutField.parseGroupsJson

Once groups are loaded, the frontend can simply request:

```
GET /users/1?group=profileScreen
```

Not only entities
-----------------

While how Jalia handles entities is peculiar, it handles correctly also:
- any POJO, simply it will not be augmented with "@entity" and "id"
- any primitive value, more specifically
 - any number
 - any CharSequence, including obviously String
 - booleans
 - any Enum, the enum value name is used
 - Class instances, the fully qualified name will be serialized
 - UUIDs, serialized as strings
 - Date, serialized as timestamp in milliseconds
- Arrays, List and Set, of POJOs, entities or natives, also polymorphic, serialized as JSON arrays
- Maps, having Strings as key and POJOs, entities, natives or other Maps, or Lists or Sets as values.

Entity names and entity ids
---------------------------

Jalia is not bound to JPA or any other persistency library, an ObjectMapper can be configured to use:
- an EntityFactory, responsible for:
 - loading an entity when an "id" is found while deserializing (more on this later)
 - extract the id from an entity while serializing
 - prepare an entity before serialization, a useful hook to normalize or denormalize data
 - "finish" an entity after deserialization, useful hook to renormalize or redenormalize data
- an EntityNameProvider, responsible for:
 - converting a Java class to a short name used in "@entity"
 - converting a short name found in "@entity" to a Java class
- a JsonClassDataFactory, responsible for:
 - analyze Java Classes, parse annotations, product JsonClassData that will be used by serialization and
 deserialization

Jalia Spring Boot automatically installs an EntityFactory and an EntityNameProvider based on JPA, that
scans the entities configured in you application and use sensible defaults. However, if custom components
are in the Spring application context, those will be used.

The default implementation JsonDataClassFactoryImpl parses the default annotations.

Deserialization
===============

Jalia can deserialize a JSON back into Java objects. An existing instance can be given, in which case
that instance will be modified.

Deserialization of POJOs
------------------------

Suppose the following model and controller method:

```java
public enum MessageType {
  EMAIL, SMS, WHATSAPP
}

public class SendMessageRequest {
  private MessageType type;
  private String to;
  private String text;
  // Getters and setters
}

public class SendMessageResponse {
  private UUID messageId;
  private boolean successful;
  // Getters and setters
}

@RestController
@RequestMapping("/messages")
public class MessageController {

  @PostMapping
  public SendMessageResponse sendMessage(@RequestBody SendMessageRequest request) {
    // Do something, send the message
    return new SendMessageResponse(messageUuid, true);
  }
}
```

Now, a post is made:

```
POST /messages
{
  "type": "EMAIL",
  "to": "fake_email@fakesite.com",
  "text": "Hello, this is a mail"
}
```

Jalia will:
- parse the incoming JSON
- infer the proper type based on the "sendMessage" parameter
- since it's not an entity, and there is no existing value, it will create a new instance
- calling the setters, will set he values
- Spring will proceed invoking the method
- Jalia will serialize the response

Deserialization of entity values
--------------------------------

When it comes to entities, the behaviour is slightly different. Let's see a simple case first, entity
as a property of a POJO:

```java
public class SendMessageRequest {
  private MessageType type;
  private String to;
  private String text;
  private User from;

  // Getters and setters

  public void setFrom(User user) {
    this.from = user;
  }
}
```

Now, given the same controller above, the following post requests can be made:

```
POST /messages

{
  "type": "EMAIL",
  "to": "fake_email@fakesite.com",
  "test": "Hello there!",
  "from": 25
}
```

Here "from" is the id of a User, Jalia will infer the type, find only a single value in the JSON and
use the configured EntityFactory to load the corresponding entity.

The post can also be:

```
{
  "type": "EMAIL",
  "to": "fake_email@fakesite.com",
  "test": "Hello there!",
  "from": {
    "@entity": "User",
    "id": 25
  }
}
```

Both these forms supports polymorphism. Suppose there are subclasses of User, like AdministrativeUser and
SystemUser, the correct subtype will be loaded by the EntityFactory or inferred using "@entity".

For the sake of frontend simplicity, the POST can also be:

```
{
  "type": "EMAIL",
  "to": "fake_email@fakesite.com",
  "test": "Hello there!",
  "from": {
    "@entity": "User",
    "id": 25,
    "firstName": "John",
    "lastName": "Smith"
  }
}
```

By default, any other property will be ignored. This is a security precaution, otherwise by sending
a message the untrusted client could alter the User entity, like change the name, which could then be
by mistake persisted.

Also, new instances are not permitted. The following POST will throw exception, because it would create
a new User instance:

```
{
  "type": "EMAIL",
  "to": "fake_email@fakesite.com",
  "test": "Hello there!",
  "from": {
    "@entity": "User",
    "firstName": "Spammer",
    "lastName": "User"
  }
}
```

If instead it is wanted for a client to alter en entity or to create new instances of it, two annotations
can be placed:
- @JsonAllowNewInstances : permits creation of new instances
- @JsonAllowEntityPropertyChanges : permits changing of fields on an existing entity (note that this applies
only to entities, changes to POJOs are always possible, and only to exising ones, if new instances are allowed
setting properties on those new entities is allowed).

The annotations can be placed on the getter or on the setter, and can be applied to collections (List,
Set, arrays, Maps) as well. The annotations are not transitive, the permission does not cascade to other
relations.

Deserialization of full entities
--------------------------------

Now, let's add this method to the controller:

```java
@RestController @RequestMapping("/users")
public class UserController {

  @Autowired
  private UserJpaRepository users;

  @PutMapping("/{id}")
  public void updateUser(@RequestBody @IdPathRequestBody @Valid User user) {
    users.save(user);
  }
}
```

Suppose the following request:

```
PUT /users/1

{
  "@entity": "User"
  "id": 1,
  "firstName": "Jhonny"
}
```

Jalia will load the entity using the id taken from the path variable thanks to the @IdPathRequestBody, inferring the
type based on the method parameter as before. It will then modify the entity changing the "firstName", while all the
rest of the entity will be untouched.

Note that:
- it's not possible to create a new entity this way, because only the entity loaded using the "id" in the path will be
loaded, and if not found an exception is thrown
- @Valid can be used to use provided bean validation
- the "@entity" and the "id" in the JSON are optional, if id doesn't correspond an exception will be thrown
- the update cannot change any fields nor create new entities in relations, so it can't rename a group by following the
"memberOf" relation nor alter other users traversing the "following" or "followedBy" relations
- the update can however change the groups and the followers lists, using already existing entities only.

Since relations are not updated, also the "addresses" relation cannot be updated. Since instead we want this relation
to be modifiable, we can use the following annotations on the setter:

```java
@Entity
public class User {
  // Previous fields omitted
  @OneToMany
  private List<Address> addresses;

  @JsonAllowNewInstances
  @JsonAllowEntityPropertChanges
  public List<Address> getAddresses() {
    return this.addresses;
  }

  // Other getters and setters omitted
}
```

Now, an update can freely add, remove, modify existing and create new entities.

Deserialized differences
------------------------

While Jalia will magically load the entity, deserialize JSON over the existing entity applying changes, and give
you the already updated data, it will hide which changes happened during the deserialization.

Let's suppose, for the sake of an example, that you need to update an external ActiveDirectory when the user is changed,
by removing the user from previous some "city" groups when the addresses are changed. To do this, we need to
retrieve the previous list of Addresses, which is doable using the ChangeRecorder:

```java
@PutMapping("/{id}")
public void updateUser(@RequestBody @IdPathRequestBody @Valid User user) {
  Change<List<Address>> addressesChange = ChangeRecorder.getChange(user, "addresses");
  List<Address> previousAddresses = addressChange.getOldValue();
  // Use the list of previous addresses to find differences and do what you need
  users.save(user);
}

```

Note that change recording adds a moderate overhead to the deserialization, around 15%, so if your project does not
need it, configure the ObjectMapper not to record it:

```
@Bean
public ObjectMapper objectMapper() {
  ObjectMapper mapper = new ObjectMapper();
  mapper.setOption(DefaultOptions.RECORD_CHANGES, false);
  return mapper;
}

```

Deserializing new entities
--------------------------

However, we could add also the following method to the controller:

```java
@RestController @RequestMapping("/users")
public class UserController {

  @Autowired
  private UserJpaRepository users;

  @PostMapping
  public void addUser(@RequestBody @Valid User user) {
    users.save(user);
  }
}
```

All rules applied to the updateUser method applies, so the call is safe, except that a new entity will be created:

```
POST /users

{
  "firstName": "Marius",
  "lastName": "White",
  "password": "abcdef",
  "addresses": [
    {
      "street": "Black street",
      "number": 21,
      "zip": "12345"
    }
  ],
  "memberOf": [
    14,
    18
  ]
}
```

