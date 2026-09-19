package br.com.erudio.unittests.mapper.mocks

import br.com.erudio.data.dto.BookDTO
import br.com.erudio.model.Book
import java.util.*

class MockBook {

    fun mockEntity(): Book {
        return mockEntity(0)
    }

    fun mockDTO(): BookDTO {
        return mockDTO(0)
    }

    fun mockEntityList(): List<Book> {
        val books: MutableList<Book> = ArrayList()
        for (i in 0..13) {
            books.add(mockEntity(i))
        }
        return books
    }

    fun mockDTOList(): List<BookDTO> {
        val books: MutableList<BookDTO> = ArrayList()
        for (i in 0..13) {
            books.add(mockDTO(i))
        }
        return books
    }

    fun mockEntity(number: Int): Book {
        val book = Book()
        book.id = number.toLong()
        book.author = "Some Author$number"
        book.launchDate = Date()
        book.price = 25.0
        book.title = "Some Title$number"
        return book
    }

    fun mockDTO(number: Int): BookDTO {
        val book = BookDTO()
        book.id = number.toLong()
        book.author = "Some Author$number"
        book.launchDate = Date()
        book.price = 25.0
        book.title = "Some Title$number"
        return book
    }
}
