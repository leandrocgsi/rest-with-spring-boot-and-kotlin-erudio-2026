package br.com.erudio.mapper

import com.github.dozermapper.core.DozerBeanMapperBuilder
import com.github.dozermapper.core.Mapper

object DozerMapper {

    private val mapper: Mapper = DozerBeanMapperBuilder.buildDefault()

    fun <O, D> parseObject(origin: O, destination: Class<D>): D {
        return mapper.map(origin, destination)
    }

    fun <O, D> parseListObjects(origin: List<O>, destination: Class<D>): List<D> {
        val destinationObjects: MutableList<D> = ArrayList()
        for (o in origin) {
            destinationObjects.add(mapper.map(o, destination))
        }
        return destinationObjects
    }
}
