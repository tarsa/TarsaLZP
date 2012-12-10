#Technical specification

###Architecture goals
Algorithm and format are designed to fully support streaming. The input stream size is not stored in compressed stream which makes it possible to compress data without knowing its size in advance. Decoder is able to tell where encoder stopped outputting bytes for current compressed stream making it possible to easily concatenate streams or embed compressed stream in a more complex structure, without storing the length of compressed data.

Algorithm is designed to be simple and generally fast. There are no optimizations for specific types of data, eg. there are no multimedia filters, no executable filters, no text filters and models, no record models and so on. Despite simplicity being an important goal, the algorithm includes some interesting techniques.

Algorithm is designed to be efficiently executed on 32-bit and more advanced platforms. There are no non-trivial 64-bit instructions that are required for almost all options combinations. However, with highest settings the algorithm would need to allocate more than 4 GiB memory (in a single process) making it impossible to run the algorithm on 32-bit platform.

##High level overview

The complete compression scheme consists of four main parts:

 - One or two LZP models. Only one prediction is taken into consideration when coding a particular symbol. Only 0 or 1 length matches. No storing or coding of distances.
 - Quantized state map. History of matches is quantized to 8 bit state.
 - Adaptive probability map for states from each LZP model. Adaptive probability map maps the quantized state map and a small additional context to a local probability of the byte predicted by LZP model.
 - Order-1 or order-2 literal coding. If the selected LZP model wrongly predicts the next byte, then literal coder comes into play.


##Coding process in detail

 - If both LZP models are set to identical options, then the implementations allocate only one LZP model to avoid wasting of resources. Running identical models does not provide any advantage

####EOF coding

 - Before encoding each input byte and after encoding the last byte, a flag is coded that indicated if there is an end of symbols

####State tables

 - State tables provide a compact way to store quantized match histories
 - States are organized into finite state machine
 - In this implementation states are symmetric, ie for any state that describes a match history, there is a state for an inverse match history, ie with matches and misses swapped
 - State table is generated using generator and ad-hoc values
 - State table is focused on states describing either skewed or short match histories, there are no states describing long and balanced match histories

####LZP models

 - Consider the most recently processed symbols as a context
 - Context length for every LZP model is specified in options
 - Compute FNV-1 hash of context content
 - Perform a logical AND operation with a mask of size specified in options
 - The resulting values is used as an index to records table
 - Each record is 16 bits long - 8 bits for predicted symbol and 8 bits for quantized state
 - After encoding each input byte, that byte is stored in the mentioned record, so before encoding the record holds a byte that was seen in input when the index was equal to current one
 - If the prediction is correct, ie the predicted symbol is equal to the currently processed one, then we have a match of length 1
 - Otherwise we have no match (sometimes called a match of length 0)
 - The quantized state holds an approximation of recent history of matches related to the currenlty used record
 - The quantized state is updated according to the comparison status of predicted byte and actual byte from input

####Converting quantized state to probability

 - In parallel to quantized state for each record in LZP model, a local history of last four LZP matches (one per LZP model) is maintained
 - Local history is combined with quantized state to produce an index to adaptive probability map
 - Adaptive probability map is an array of probabilities
 - Probabilities are updated according to the comparison status of predicted byte and actual byte from input

####Final LZP steps

 - If there are two LZP (+APM) models then the one that reports higher probability of a successful match is selected and the other one is ignored for the currently processed symbol
 - That reported probability is used to code a flag indicating if the selected LZP model predicted the right symbol

####Literal coding

 - If LZP model predicted the wrong symbol then a last step, which is literal coding, is needed
 - Literal coder knows the symbol that was wrongly predicted by LZP model and temporarily removes it from alphabet when coding the input symbol

####Incompressible sequences handling

 - Literal coder maintains a weighted cost estimator
 - Weighted cost is a weighted sum of previous costs of input symbols that were not predicted correctly by LZP layer
 - After a symbol is processed by literal coder, a weighted cost is updated with its cost
 - Before encoding a symbol, literal coder checks the weighted cost estimation and when it exceeds a threshold indicating an incompressible sequence, then it codes the symbol using fixed probability of 1/ 255
 - However, the updating of frequencies and weighted cost estimation is done in the same way every time the literal coder is invoked

####Speed optimizations

 - Symbol frequencies in literal coder are grouped in 16 symbol groups to speed up computing cumulative frequencies on encoding and finding original symbol when decoding
 - The grouping has no effect on the contents of compressed stream
 - C version optionally uses vector optimizations in the form of x86 SSE2 intrinsics at encoding phase
 - Similarly, C version uses prefetching from SSE2 instruction set at encoding phase to load memory areas needed for encoding the subsequent input symbol

##Possible performance improvements

 - Using block based encoding:
  - Firstly process a chunk of the input data with one LZP model and store the computed values in a buffer
  - Then do that for the second LZP model, if there are two models used
  - Then do adaptive probability mapping for each of the resulting buffers
  - Then do the flags coding together with literal coding
  - This way we can utilize CPU cache better because the cache will hold only one type of data at each pass
 - Using multiple threads:
  - Above scheme can be easily parallelized by pipelining
