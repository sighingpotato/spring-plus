package org.example.expert.domain.todo.repository;

import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.comment.entity.QComment;
import org.example.expert.domain.manager.entity.QManager;
import org.example.expert.domain.todo.dto.response.TodoSearchResponseDto;
import org.example.expert.domain.todo.entity.QTodo;
import org.example.expert.domain.todo.entity.Todo;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.example.expert.domain.user.entity.QUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

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

  @Override
  public Page<TodoSearchResponseDto> searchTodos(String title, LocalDateTime startDate, LocalDateTime endDate, String nickname, Pageable pageable) {
    QTodo todo = QTodo.todo;
    QManager manager = QManager.manager;
    QComment comment = QComment.comment;
    QUser user = QUser.user; // 담당자 닉네임을 찾기 위해 Manager와 연결된 User 정보 필요

    // 1. 실제 데이터를 가져오는 메인 쿼리
    List<TodoSearchResponseDto> results = queryFactory
        // 엔티티 전체가 아니라 DTO에 필요한 딱 3가지 값만 뽑아온다
        .select(Projections.constructor(TodoSearchResponseDto.class,
            todo.title,
            manager.countDistinct(), // 중복 제거된 담당자 수
            comment.countDistinct()  // 중복 제거된 댓글 수
        ))
        .from(todo)
        // 담당자, 유저, 댓글 테이블을 조인한다
        .leftJoin(todo.managers, manager)
        .leftJoin(manager.user, user)
        .leftJoin(todo.comments, comment)
        // 동적 조건 검색
        .where(
            titleContains(todo, title),
            dateBetween(todo, startDate, endDate),
            nicknameContains(user, nickname)
        )
        // 그룹으로 묶어서 count() 연산이 가능하게 한다.
        .groupBy(todo.id)
        // 최신순 정렬 및 페이징 설정
        .orderBy(todo.createdAt.desc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    // 2. 전체 데이터 개수를 세는 카운트 쿼리
    JPAQuery<Long> countQuery = queryFactory
        .select(todo.count())
        .from(todo)
        .leftJoin(todo.managers, manager)
        .leftJoin(manager.user, user)
        .where(
            titleContains(todo, title),
            dateBetween(todo, startDate, endDate),
            nicknameContains(user, nickname)
        );

    // 3. Page 객체로 예쁘게 포장해서 반환
    return PageableExecutionUtils.getPage(results, pageable, countQuery::fetchOne);
  }

  // 동적 쿼리를 위한 도우미 메서드들 (값이 없으면 null을 반환하여 조건을 무시함) ---
  private BooleanExpression titleContains(QTodo todo, String title) {
    return StringUtils.hasText(title) ? todo.title.contains(title) : null;
  }

  private BooleanExpression dateBetween(QTodo todo, LocalDateTime startDate, LocalDateTime endDate) {
    if (startDate != null && endDate != null) {
      return todo.createdAt.between(startDate, endDate);
    }
    return null;
  }

  private BooleanExpression nicknameContains(QUser user, String nickname) {
    return StringUtils.hasText(nickname) ? user.nickname.contains(nickname) : null;
  }
}
