package com.example.testime

import android.inputmethodservice.InputMethodService
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class CandidateLibrary(private val inputMethod: InputMethod) : InputMethodService() {
	data class Candidate(val input: String, val output: String)

	private var candidateList = emptyList<Candidate>()
	private var mappingTable = emptyMap<String, String>()
	private var functionKeyMap: Map<String, Int>? = null
	private var symbolList = emptyList<List<String>>()

	fun loadWords(id: Int) {
		try {
			val inputStream: InputStream = inputMethod.resources.openRawResource(id)
			val candidates = mutableListOf<Candidate>()
			BufferedReader(InputStreamReader(inputStream)).use { reader ->
				var line: String?
				while (reader.readLine().also { line = it } != null) {
					val parts = line?.split("\t") ?: continue
					if (parts.size == 2) {
						if (parts[1].length > 1) for (i in 0 until parts[1].length) {
							candidates.add(Candidate(parts[0], parts[1][i].toString()))
						}
						else candidates.add(Candidate(parts[0], parts[1]))
					}
				}
			}
			candidateList = candidates
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	fun loadMapping(id: Int) {
		try {
			val inputStream: InputStream = inputMethod.resources.openRawResource(id)
			val mapping = mutableMapOf<String, String>()
			BufferedReader(InputStreamReader(inputStream)).use { reader ->
				var line: String?
				while (reader.readLine().also { line = it } != null) {
					val parts = line?.split(" ") ?: continue
					if (parts.size == 2) {
						mapping[parts[1]] = parts[0]
					}
				}
			}
			mappingTable = mapping
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	fun loadFunKeyMap(keyMap: Map<String, Int>) {
		functionKeyMap = keyMap
	}

	fun loadSymbolList(list: List<List<String>>) {
		symbolList = list
	}

	fun getKeyValue(key: String): Int? {
		return functionKeyMap?.get(key)
	}

	fun getMatchCandidates(input: String): List<String> {
		val splitInput = input.split(" ")
		val convertInput = splitInput.joinToString("") { mappingTable[it] ?: it }
		val filteredCandidates = candidateList.filter { it.input == convertInput }
		return filteredCandidates.map { it.output }
	}

	fun getSymbol(index: Int, pos: Int): String {
		if(pos < symbolList[index].size) {
			return symbolList[index][pos]
		}
		return ""
	}

	fun getSymIndices(index: Int): IntRange {
		return symbolList[index].indices
	}
}