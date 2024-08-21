package com.example.redis;

import com.example.redis.domain.Item;
import com.example.redis.domain.ItemDto;
import com.example.redis.repo.ItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ItemService {
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }


    @Cacheable(cacheNames = "itemAllCache", key = "methodName")
    public List<ItemDto> readAll() {
        List<ItemDto> itemDtoList = itemRepository.findAll().stream().map(ItemDto::fromEntity).toList();
        return itemDtoList;
        
        //위에걸 줄이면 아래와 같다
        /*return itemRepository.findAll()
                .stream()
                .map(ItemDto::fromEntity)
                .toList();*/
    }

    //cache-aside 전략 : 데이터 조회시 캐시먼저 조회후 캐시에 있으면 가져오고, 캐시 없으면 원본을 가져온 후 캐시에 저장함
    //cacheNames : 적용할 캐시 규칙을 지정하기 위한 이름 -> 1.컨피그에서 어떤 설정을 사용할 것인지 지칭,
    //2.다른 어노테이션에서 이 캐싱데이터를 활용할 필요있을때 도 이 이름을 찾아보게 됨
    //key : 캐시데이터를 확인하기 위해 활용하는 값
    //args[0] : spring expression language라고 하는 스프링 고유 문법. 인자(파라미터-id)들중 첫번째를 가져와서 키에 활용할 것이다 란 의미임
    @Cacheable(cacheNames = "itemCache", key = "args[0]")
    public ItemDto readOne(Long id) {
        log.info("Read One: {}", id); //캐시를 가져올 때는 요청이 들어가도 이 메서드 자체를 실행x -> 로그도 찍히지 않음

        ItemDto itemDto = itemRepository.findById(id).map(ItemDto::fromEntity).orElseThrow(()
                -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return itemDto;

        //위에걸 아래것처럼 바꿀 수 있다.
        /*return itemRepository.findById(id)
                .map(ItemDto::fromEntity) //entity를 dto로 변경함
                //.map(item -> ItemDto.fromEntity(item)) 위와 같은 의미이다.
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));*/
    }

    //CachePut은 write-through 방식이다 (참고로 write-behind는 어노테이션 따로 없음 직접 구현해야함)
    @CachePut(cacheNames = "itemCache", key = "#result.id") //result.id =itemDto.id=41(40개 들어가있으므로)
    public ItemDto create(ItemDto dto) { //requestDto와 responseDto 가 같음 주의!
        Item item = Item.builder().name(dto.getName()).description(dto.getDescription()).price(dto.getPrice()).build();
        itemRepository.save(item);
        return ItemDto.fromEntity(item);
        
        //위에걸 줄이면 아래와 같음
        /*Item item = itemRepository.save(Item.builder().name(dto.getName()).description(dto.getDescription()).price(dto.getPrice()).build());
        return ItemDto.fromEntity(item);*/
        
        //위에걸 줄이면 아래와 같음
        /*return ItemDto.fromEntity(itemRepository.save(Item.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .build()));*/
    }

    @CachePut(cacheNames = "itemCache", key = "args[0]") //key = "#result.id"해도 결과는 같을 것임
    @CacheEvict(cacheNames = "itemAllCache", allEntries = true) //하나 업데이트했으므로 readAll시 캐시만든건 최신정보 아니므로 없애줘야함
    //allEntries = true : itemAllCache로 시작하는 모든 데이터를 제거하겠다는 의미
    //@CacheEvict(cacheNames = "itemAllCache", key = "'readAll'") 이렇게해서 특정 키만 삭제해 줄 수도 있다.
    public ItemDto update(Long id, ItemDto dto) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());

        Item updatedItem = itemRepository.save(item);
        return ItemDto.fromEntity(updatedItem);

        //아래처럼 쓸 수 있다
        //return ItemDto.fromEntity(itemRepository.save(item));
    }

    //@CacheEvict(cacheNames = "itemCache", key = "args[0]")
    //@CacheEvict(cacheNames = "itemAllCache", allEntries = true)
    //위의 2개를 동시에 쓸 수 없으므로 아래처럼 해주었다
    //@CacheEvict(cacheNames = {"itemCache", "itemAllCache"}, allEntries = true)
    // 이방법이 더 좋다
    @Caching(evict = {@CacheEvict(cacheNames = "itemCache", key = "args[0]"),
                      @CacheEvict(cacheNames = "itemAllCache", allEntries = true)})
    public void delete(Long id) {
        itemRepository.deleteById(id);
    }


    @Cacheable(cacheNames = "itemSearchCache", key = "{ args[0], args[1].pageNumber, args[1].pageSize }")
    public Page<ItemDto> searchByName(String query, Pageable pageable) {
        Page<ItemDto> itemDto = itemRepository.findAllByNameContains(query, pageable).map(ItemDto::fromEntity);
        return itemDto;

        /*return itemRepository.findAllByNameContains(query, pageable)
                .map(ItemDto::fromEntity);*/
    }

}
