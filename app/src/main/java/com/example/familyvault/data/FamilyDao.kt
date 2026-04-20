package com.example.familyvault.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {

    @Insert
    suspend fun insertMember(member: FamilyMember)

    @Delete
    suspend fun deleteMember(member: FamilyMember)
    // In your FamilyDao interface
    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getMemberById(id: Int): FamilyMember?
    @Query("SELECT * FROM family_members")
    fun getAllMembers(): Flow<List<FamilyMember>>
}