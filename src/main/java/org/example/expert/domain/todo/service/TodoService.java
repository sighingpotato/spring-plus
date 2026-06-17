package org.example.expert.domain.todo.service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final WeatherClient weatherClient;

    @Transactional // 기본값인 readOnly = false 적용
    public TodoSaveResponse saveTodo(AuthUser authUser, TodoSaveRequest todoSaveRequest) {
        User user = User.fromAuthUser(authUser);

        String weather = weatherClient.getTodayWeather();

        Todo newTodo = new Todo(
                todoSaveRequest.getTitle(),
                todoSaveRequest.getContents(),
                weather,
                user
        );
        Todo savedTodo = todoRepository.save(newTodo);

        return new TodoSaveResponse(
                savedTodo.getId(),
                savedTodo.getTitle(),
                savedTodo.getContents(),
                weather,
                new UserResponse(user.getId(), user.getEmail())
        );
    }

    public Page<TodoResponse> getTodos(int page, int size, String weather, String startDateStr, String endDateStr) {
        Pageable pageable = PageRequest.of(page - 1, size);

        // 1. String 날짜 데이터를 LocalDateTime 범위로 변환 처리
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        if (StringUtils.hasText(startDateStr) && StringUtils.hasText(endDateStr)) {
            startDate = LocalDate.parse(startDateStr).atStartOfDay();      // 00:00:00
            endDate = LocalDate.parse(endDateStr).atTime(LocalTime.MAX);    // 23:59:59.999
        }

        // 2. 조건 존재 여부 체크
        boolean hasWeather = StringUtils.hasText(weather);
        boolean hasPeriod = (startDate != null);

        Page<Todo> todos;

        // 3. 기획 요건에 따른 if문 분기 처리
        if (hasWeather && hasPeriod) {
            todos = todoRepository.findAllByWeatherAndModifiedAtBetween(weather, startDate, endDate, pageable);
        } else if (hasWeather) {
            todos = todoRepository.findAllByWeather(weather, pageable);
        } else if (hasPeriod) {
            todos = todoRepository.findAllByModifiedAtBetween(startDate, endDate, pageable);
        } else {
            // 아무 조건도 없을 때는 기존처럼 수정일 기준 내림차순 전체 조회
            todos = todoRepository.findAllByOrderByModifiedAtDesc(pageable);
        }

        return todos.map(todo -> new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContents(),
                todo.getWeather(),
                new UserResponse(todo.getUser().getId(), todo.getUser().getEmail()),
                todo.getCreatedAt(),
                todo.getModifiedAt()
        ));
    }

    public TodoResponse getTodo(long todoId) {
        Todo todo = todoRepository.findByIdWithUser(todoId)
                .orElseThrow(() -> new InvalidRequestException("Todo not found"));

        User user = todo.getUser();

        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContents(),
                todo.getWeather(),
                new UserResponse(user.getId(), user.getEmail()),
                todo.getCreatedAt(),
                todo.getModifiedAt()
        );
    }
}
