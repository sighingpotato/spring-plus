package org.example.expert.domain.todo.repository;

import java.util.Optional;
import org.example.expert.domain.todo.entity.Todo;

public interface TodoRepositoryCustom {
  // JPQL로 작성했던 메서드 이름과 동일하게 선언
  Optional<Todo> findByIdWithUser(Long todoId);

}
