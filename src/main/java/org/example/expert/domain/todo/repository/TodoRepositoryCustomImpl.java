package org.example.expert.domain.todo.repository;

import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.todo.entity.Todo;
import com.querydsl.jpa.impl.JPAQueryFactory;

@RequiredArgsConstructor
public class TodoRepositoryCustomImpl implements TodoRepositoryCustom{

  // QueryDSL을 사용하기 위해 필요한 핵심 클래스
  private final JPAQueryFactory queryFactory;

  @Override
  public Optional<Todo> findByIdWithUser(Long todoId) {
    Todo result = queryFactory
        .selectFrom(todo)
        .leftJoin(todo.user, user).fetchJoin()
        .where(todo.id.eq(todoId))
        .fetchOne(); // 단건 조회

    return Optional.ofNullable(result);
  }

}
