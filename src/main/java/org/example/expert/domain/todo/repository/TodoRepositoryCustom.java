package org.example.expert.domain.todo.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.example.expert.domain.todo.dto.response.TodoSearchResponseDto;
import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TodoRepositoryCustom {
  // JPQL로 작성했던 메서드 이름과 동일하게 선언
  Optional<Todo> findByIdWithUser(Long todoId);

  Page<TodoSearchResponseDto> searchTodos(
      String title,
      LocalDateTime startDate,
      LocalDateTime endDate,
      String nickname,
      Pageable pageable
  );
}
