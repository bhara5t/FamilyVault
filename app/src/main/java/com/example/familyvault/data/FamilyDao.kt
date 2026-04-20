package com.example.familyvault.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {

    @Insert
    suspend fun insertMember(member: FamilyMember)

    @Delete
    suspend fun deleteMember(member: FamilyMember)

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getMemberById(id: Int): FamilyMember?

    @Query("SELECT * FROM family_members")
    fun getAllMembers(): Flow<List<FamilyMember>>

    // Sync version for import operations
    @Query("SELECT * FROM family_members")
    suspend fun getAllMembersSync(): List<FamilyMember>
}