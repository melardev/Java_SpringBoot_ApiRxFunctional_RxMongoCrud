package com.melardev.spring.crud.controllers;


import com.melardev.spring.crud.entities.Todo;
import com.melardev.spring.crud.repositories.TodosRepository;
import com.melardev.spring.crud.responses.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Component
@CrossOrigin
public class TodoRequestHandler {

    @Autowired
    TodosRepository todosRepository;


    public Mono<ServerResponse> getAll(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(todosRepository.findAllHqlSummary(), Todo.class);
    }


    public Mono<ServerResponse> getPending(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(todosRepository.findByCompletedFalse(), Todo.class);
    }


    public Mono<ServerResponse> getCompleted(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(todosRepository.findByCompletedIsTrueHql(), Todo.class);
    }


    public Mono<ServerResponse> getById(ServerRequest request) {
        String id = request.pathVariable("id");

        return this.todosRepository.findById(id)
                .flatMap((Function<Todo, Mono<ServerResponse>>) todo -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Mono.just(todo), Todo.class))
                .switchIfEmpty(ServerResponse
                        .status(HttpStatus.NOT_FOUND)
                        .body(Mono.just(new ErrorResponse("Todo not found")), ErrorResponse.class));
    }


    public Mono<ServerResponse> create(ServerRequest request) {
        Mono<Todo> todo = request.bodyToMono(Todo.class);

        // Longer but more readable
        /*
        return todo.flatMap(new Function<Todo, Mono<Todo>>() {
            @Override
            public Mono<Todo> apply(Todo todo) {
                return todosRepository.save(todo);
            }
        }).flatMap(new Function<Todo, Mono<ServerResponse>>() {
            @Override
            public Mono<ServerResponse> apply(Todo todo) {
                return ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Mono.just(todo), Todo.class);
            }
        });
        */
        // Shorter way
        return todo.flatMap((Function<Todo, Mono<Todo>>) todoInput -> todosRepository.save(todoInput))
                .flatMap((Function<Todo, Mono<ServerResponse>>) savedTodo -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Mono.just(savedTodo), Todo.class));
    }


    public Mono<ServerResponse> update(ServerRequest request) {
        String id = request.pathVariable("id");
        Mono<Todo> todoInput = request.bodyToMono(Todo.class);

        /* Longer approach
        return todosRepository.findById(id)
                .flatMap(new Function<Todo, Mono<Todo>>() {
                    @Override
                    public Mono<Todo> apply(Todo todoFromDb) {
                        return todoInput.flatMap(new Function<Todo, Mono<Todo>>() {
                            @Override
                            public Mono<Todo> apply(Todo todo) {
                                String title = todo.getTitle();
                                if (title != null)
                                    todoFromDb.setTitle(title);

                                String description = todo.getDescription();
                                if (description != null)
                                    todoFromDb.setDescription(description);

                                todoFromDb.setCompleted(todo.isCompleted());
                                return todosRepository.save(todoFromDb);
                            }
                        });
                    }
                })
                .flatMap(new Function<Todo, Mono<ServerResponse>>() {
                    @Override
                    public Mono<ServerResponse> apply(Todo todo) {
                        return ServerResponse.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                                .body(Mono.just(todo), Todo.class);
                    }
                })
                .switchIfEmpty(ServerResponse
                        .status(HttpStatus.NOT_FOUND)
                        .body(Mono.just(new ErrorResponse("Todo not found")), ErrorResponse.class));
*/

        // Shorter but more readable
        return todosRepository.findById(id)
                .flatMap(t -> todoInput.flatMap((Function<Todo, Mono<Todo>>) todo -> {
                    String title = todo.getTitle();
                    if (title != null)
                        t.setTitle(title);

                    String description = todo.getDescription();
                    if (description != null)
                        t.setDescription(description);

                    t.setCompleted(todo.isCompleted());
                    return todosRepository.save(t);
                }))
                .flatMap(todo -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Mono.just(todo), Todo.class))
                .switchIfEmpty(ServerResponse
                        .status(HttpStatus.NOT_FOUND)
                        .body(Mono.just(new ErrorResponse("Todo not found")), ErrorResponse.class));

    }

    @DeleteMapping("/{id}")
    public Mono<ServerResponse> delete(ServerRequest request) {
        String id = request.pathVariable("id");
        return todosRepository.findById(id)
                .flatMap(t -> todosRepository.delete(t)
                        .then(ServerResponse.noContent().build()))
                .switchIfEmpty(ServerResponse
                        .status(HttpStatus.NOT_FOUND)
                        .body(Mono.just(new ErrorResponse("Todo not found")), ErrorResponse.class));
    }


    @DeleteMapping
    public Mono<ServerResponse> deleteAll(ServerRequest request) {
        return todosRepository.deleteAll().then(ServerResponse.noContent().build());
    }

}