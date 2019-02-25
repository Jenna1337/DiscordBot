#SingleInstance force
#NoEnv
#Warn  ; Enable warnings to assist with detecting common errors.
SetWorkingDir %A_ScriptDir%
SetKeyDelay, 200

inputfile := "blacklists.ini"
outputfile := "blacklistproviders.tsv"
debug := false
verbose := false
optionalparlist := ["inputfile","outputfile","debug","verbose"]

for n, param in A_Args  ; For each parameter:
{
	aar := ""+param
	parr := StrSplit(aar, "`=",, MaxParts := 2)
	
	if parr.Length()<2
	{
		continue
	}
	vname := parr[1]
	vval := parr[2]
	for n, optionalpar in optionalparlist
	{
		if vname = %optionalpar%
		{
			%vname% := vval
			;MsgBox % vname "`=" vval
		}
	}
}
StringLower, debug, debug
if(debug==true or debug=="true")
{
	MsgBox % inputfile ">" outputfile
}
if(verbose==true or verbose=="true")
{
	FileAppend, # %inputfile% > %outputfile%`r`n, *
}

file := FileOpen(outputfile, "w")
file.WriteLine("#[NAME]	[FORMAT]	[URL]")

IniRead, SectionNames, %inputfile%
SectionNameArray := StrSplit(SectionNames, "`n")
Loop % SectionNameArray.Length()
{
	SectionName := % SectionNameArray[A_Index]
	IniRead, SectionFormat, %inputfile%, %SectionName%, FORMAT
	IniRead, SectionUrl, %inputfile%, %SectionName%, URL
	file.WriteLine(SectionName "`t" SectionFormat "`t" SectionUrl)
}

file.Close()

;stdout := FileOpen("*", "w")
FileAppend, %outputfile%, *
;stdout.WriteLine(outputfile)
;stdout.Close()
