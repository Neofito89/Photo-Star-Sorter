package com.example

import android.net.Uri
import com.example.data.model.PhotoItem
import com.example.data.model.RatingFilter
import com.example.data.model.RatingStatus
import com.example.data.model.SortDirection
import com.example.data.model.SortField
import com.example.data.model.SortOrder
import com.example.ui.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.max
import kotlin.math.min

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FilteringAndSortingTest {

    private val samplePhotos = listOf(
        createPhoto("IMG_0001.JPG", rating = 5, status = RatingStatus.RATED, date = 1000L, size = 100L),
        createPhoto("IMG_0002.CR3", rating = 3, status = RatingStatus.RATED, date = 2000L, size = 500L),
        createPhoto("IMG_0003.CR2", rating = 5, status = RatingStatus.RATED, date = 3000L, size = 400L),
        createPhoto("IMG_0004.JPG", rating = 0, status = RatingStatus.UNRATED, date = 4000L, size = 200L),
        createPhoto("IMG_0005.CR3", rating = null, status = RatingStatus.UNAVAILABLE, date = 5000L, size = 600L),
        createPhoto("IMG_0006.JPG", rating = 1, status = RatingStatus.RATED, date = 6000L, size = 150L)
    )

    private fun createPhoto(
        name: String,
        rating: Int?,
        status: RatingStatus,
        date: Long,
        size: Long
    ): PhotoItem {
        return PhotoItem(
            uri = Uri.parse("content://com.android.externalstorage.documents/document/$name"),
            uriString = "content://com.android.externalstorage.documents/document/$name",
            fileName = name,
            relativePath = "DCIM/100CANON",
            fileSize = size,
            lastModified = date,
            mimeType = "image/jpeg",
            rating = rating,
            ratingStatus = status,
            captureDate = date
        )
    }

    @Test
    fun filter_All_returnsAllPhotos() {
        val result = MainViewModel.applyFilterAndSort(
            samplePhotos,
            RatingFilter.All,
            SortOrder()
        )
        assertEquals(samplePhotos.size, result.size)
    }

    @Test
    fun filter_Exact5_returnsOnly5StarPhotos() {
        val result = MainViewModel.applyFilterAndSort(
            samplePhotos,
            RatingFilter.Exact(5),
            SortOrder()
        )
        assertEquals(2, result.size)
        assertTrue(result.all { it.rating == 5 })
    }

    @Test
    fun filter_Exact0_returnsOnlyUnratedPhotos() {
        val result = MainViewModel.applyFilterAndSort(
            samplePhotos,
            RatingFilter.Exact(0),
            SortOrder()
        )
        assertEquals(1, result.size)
        assertEquals("IMG_0004.JPG", result.first().fileName)
    }

    @Test
    fun filter_Unavailable_returnsOnlyUnavailablePhotos() {
        val result = MainViewModel.applyFilterAndSort(
            samplePhotos,
            RatingFilter.Unavailable,
            SortOrder()
        )
        assertEquals(1, result.size)
        assertEquals(RatingStatus.UNAVAILABLE, result.first().ratingStatus)
    }

    @Test
    fun sort_byRatingDescending_places5StarsFirst() {
        val result = MainViewModel.applyFilterAndSort(
            samplePhotos,
            RatingFilter.All,
            SortOrder(SortField.RATING, SortDirection.DESCENDING)
        )
        // Las de 5 estrellas deben ser las primeras
        assertEquals(5, result[0].rating)
        assertEquals(5, result[1].rating)
        assertEquals(3, result[2].rating)
        assertEquals(1, result[3].rating)
    }

    @Test
    fun sort_byRatingAscending_placesLowestOrUnavailableFirst() {
        val result = MainViewModel.applyFilterAndSort(
            samplePhotos,
            RatingFilter.All,
            SortOrder(SortField.RATING, SortDirection.ASCENDING)
        )
        // Rating null (-1) va primero en ascendente
        assertEquals(null, result[0].rating)
        assertEquals(0, result[1].rating)
        assertEquals(1, result[2].rating)
    }

    @Test
    fun sort_byNameAscending_ordersAlphabetically() {
        val result = MainViewModel.applyFilterAndSort(
            samplePhotos,
            RatingFilter.All,
            SortOrder(SortField.NAME, SortDirection.ASCENDING)
        )
        assertEquals("IMG_0001.JPG", result.first().fileName)
        assertEquals("IMG_0006.JPG", result.last().fileName)
    }

    @Test
    fun rangeSelection_selectsAllPhotosBetweenIndicesInclusive() {
        val sortedList = MainViewModel.applyFilterAndSort(
            samplePhotos,
            RatingFilter.All,
            SortOrder(SortField.NAME, SortDirection.ASCENDING)
        )

        // Seleccionar rango entre índice 1 (IMG_0002.CR3) e índice 4 (IMG_0005.CR3)
        val startIndex = 1
        val endIndex = 4

        val minIdx = min(startIndex, endIndex)
        val maxIdx = max(startIndex, endIndex)
        val rangeSelected = sortedList.subList(minIdx, maxIdx + 1).map { it.fileName }

        assertEquals(4, rangeSelected.size)
        assertEquals(listOf("IMG_0002.CR3", "IMG_0003.CR2", "IMG_0004.JPG", "IMG_0005.CR3"), rangeSelected)
    }

    @Test
    fun rangeSelection_reverseIndices_yieldsSameSelection() {
        val sortedList = MainViewModel.applyFilterAndSort(
            samplePhotos,
            RatingFilter.All,
            SortOrder(SortField.NAME, SortDirection.ASCENDING)
        )

        val startIndex = 4
        val endIndex = 1

        val minIdx = min(startIndex, endIndex)
        val maxIdx = max(startIndex, endIndex)
        val rangeSelected = sortedList.subList(minIdx, maxIdx + 1).map { it.fileName }

        assertEquals(4, rangeSelected.size)
        assertEquals("IMG_0002.CR3", rangeSelected.first())
        assertEquals("IMG_0005.CR3", rangeSelected.last())
    }
}
