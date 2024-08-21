package com.example.redis;

import com.example.redis.domain.ItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    public ItemDto create(@RequestBody ItemDto itemDto) {
        return itemService.create(itemDto);
    }

    @GetMapping
    public List<ItemDto> readAll() {
        return itemService.readAll();
    }

    @GetMapping("{id}")
    public ItemDto readOne(@PathVariable("id") Long id) {
        return itemService.readOne(id);
    }

    @PutMapping("{id}")
    public ItemDto update(@PathVariable("id") Long id, @RequestBody ItemDto dto) {
        return itemService.update(id, dto);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        itemService.delete(id);
    }

    //이거 위해 어플리케이션에 EnableSpringDataWebSupport 어노테이션 추가해줌
    // items/search?q=mo (mouse)
    //items/search?q=monitor&page=0&size=5
    //-> itemSearchCache::mo,0,10
    @GetMapping("search")
    public Page<ItemDto> search(@RequestParam(name = "q") String query, Pageable pageable) {
        return itemService.searchByName(query, pageable);
    }
}

/*get localhost:8080/items/1 -> readOne -> 처음 속도가 느리고 10초동안 빠르다.
이때 itemCache::1 로 키값이 들어간다.
레디스에 itemCache::1 가 있을 경우 결과를 반환하고 레디스에 키값이 없을 경우 select 쿼리를 실행하는것을 알수있다.(콘솔에 쿼리 확인) (->cache aside 전략)

get localhost:8080/items -> read all
->itemAllCache::readAll 키가 생긴다

post localhost:8080/items -> create
itemCache::41 키가 생성됨
create하고 10초안에 read 41번 하면 readOne메서드가 동작하지 않고 캐시에서 가져온다.(콘솔에 쿼리 x)

--캐시 2분(120)로 바꾸고 진행해보기
put localhost:8080/items/1 - update
itemCache::1 생기고 itemAllCache::readAll이 레디스에서 제거됨
*/