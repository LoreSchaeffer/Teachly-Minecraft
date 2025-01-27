# Teachly Plugin

### Example JS
```javascript
const EXERCISES = [{
    createdAt: "2024-12-26T23:35:38Z",
    lastModified: "2024-12-26T23:35:38Z",
    authorId: "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    author: {
        createdAt: "2024-12-26T23:35:38Z",
        lastModified: "2024-12-26T23:35:38Z",
        id: "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        username: "teachly",
        picture: "https://picsum.photos/64",
        lastLogin: "2024-12-26T23:35:38Z"
    },
    id: "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    name: "Pythagorean theorem - exercise 1",
    question: "What is the hypotenuse of a right triangle with catheti of 6 and 8?",
    type: "OPEN_QUESTION",
    difficulty: "EASY",
    tags: [
        "math",
        "geometry"
    ],
    hints: [
        "Try to use the Pythagorean theorem"
    ],
    solutions: [
        "10",
        "10cm",
        "10 cm"
    ],
    generatorId: "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    generator: {
        createdAt: "2024-12-26T23:35:38Z",
        lastModified: "2024-12-26T23:35:38Z",
        authorId: "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        author: {
            createdAt: "2024-12-26T23:35:38Z",
            lastModified: "2024-12-26T23:35:38Z",
            id: "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            username: "teachly",
            picture: "https://picsum.photos/64",
            lastLogin: "2024-12-26T23:35:38Z"
        },
        id: "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        name: "Pythagorean triples generator",
        description: "This generator creates exercises about Pythagorean triples",
        tags: [
            "math",
            "geometry"
        ],
        blocklyJsonCode: "{\"blocks\": {\"languageVersion\": 0,\"blocks\": [{...}]}}",
        lastGeneration: "2024-12-26T23:35:38Z"
    }
}];

function onPlayerJump(event) {
    $.broadcast("<rainbow>" + event.getPlayer().getName() + " jumped!</rainbow>");
    let exercise = EXERCISES[Math.floor(Math.random() * EXERCISES.length)];
    $.exercise(event.getPlayer(), JSON.stringify(exercise));
}

function onCorrectAnswer(event) {
    $.sendMessage(event.getPlayer());
    $.sendMessage(event.getAnswer());
    $.sendMessage(event.getExercise());
}

function onWrongAnswer(event) {
    $.sendMessage(event.getPlayer());
    $.sendMessage(event.getAnswer());
    $.sendMessage(event.getExercise());
}

$.info("<green>Loading Test script!");

$.subscribe("PlayerJumpEvent", "onPlayerJump");
$.subscribe("CorrectAnswerEvent", "onCorrectAnswer");
$.subscribe("WrongAnswerEvent", "onWrongAnswer");
```